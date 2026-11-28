# API 명세

구현된 API의 요청·응답 계약입니다. 노션 MVC 표가 설계라면 이 문서는 **구현 결과**이고, 둘이 다르면 여기가 기준입니다.

## 규칙

- API를 추가·변경하면 **같은 PR에서** 이 문서도 고칩니다 (PR 템플릿 체크박스)
- [목록](#목록)에 한 줄 + Controller 섹션에 [작성 틀](#작성-틀)대로 상세
- URI: `/api/` + 복수 명사, 상태 변경은 `POST /api/자원/{id}/동작`, **memberId는 받지 않고 세션에서**
- 오류는 `ErrorCode` 이름 그대로. 새 코드가 필요하면 다은님께 먼저 요청

## 공통

**로그인 표시** — `-` 불필요 / `로그인` 안 했으면 `401 AUTH_REQUIRED` / `관리자` 아니면 `403 FORBIDDEN`

**오류 응답** — 실패하면 항상 이 모양. 화면 분기는 `code`로 (`message`는 바뀔 수 있음)

```json
{ "code": "LOGIN_FAILED", "message": "아이디 또는 비밀번호가 올바르지 않습니다." }
```

깨진 JSON(`400 BAD_REQUEST`), 서버 오류(`500 INTERNAL_ERROR`)처럼 모든 API에 공통인 오류는 상세에 적지 않습니다. 전체 목록: `common/error/ErrorCode.java`

업로드 용량 초과(`413 FILE_TOO_LARGE`)는 파일을 받는 API에 해당합니다. 파일당 10MB / 요청 전체 20MB가 서버 한도이고, API별 세부 한도는 상세에 적습니다.

## 목록

| ID | Method | URI | 설명 | 로그인 | 담당 |
|---|---|---|---|---|---|
| M2 | POST | `/api/auth/login` | 로그인 | - | 현지 |
| M2 | POST | `/api/auth/logout` | 로그아웃 | - | 현지 |
| M2 | GET | `/api/members/me` | 내 로그인 정보 | 로그인 | 현지 |
| M1 | POST | `/api/members` | 회원가입 | - | 현지 |
| M1 | GET | `/api/members/check-id` | 아이디 중복확인 | - | 현지 |
| M4 | GET | `/api/performances` | 공연 목록 (장르, 페이지) | - | 현지 |
| M9 | POST | `/api/performances` | 공연 등록 (좌석 배치 M20 포함, 심사중으로 생성) | 로그인 | 재원 |

---

## AuthController

### `POST /api/auth/login` 로그인
- 요청: `{ "loginId": string, "password": string }` 둘 다 필수
- 성공: `200` [LoginMember](#loginmember) + 세션 쿠키 발급
- 오류: `400 BAD_REQUEST` 빈 값 · `401 LOGIN_FAILED` 없는 아이디 또는 틀린 비밀번호 (구분하지 않음)

### `POST /api/auth/logout` 로그아웃
- 요청: 없음
- 성공: `204` (이미 로그아웃이어도 204)

## MemberController

### `GET /api/members/me` 내 로그인 정보
새로고침 후 로그인 상태 복원용. DB 조회 없이 세션 값을 돌려줍니다.
- 성공: `200` [LoginMember](#loginmember)
- 오류: `401 AUTH_REQUIRED` 로그인 안 함·세션 만료(30분)

### `POST /api/members` 회원가입
일반회원(USER)으로 가입합니다. 자동 로그인은 하지 않습니다.
- 요청: `{ "loginId": string, "password": string, "memberName": string, "email": string }` 모두 필수
  - `loginId` 영문·숫자 4~20자 · `password` 8자 이상 · `memberName` 공백만 불가 · `email` 형식만 검사
- 성공: `201` `{ "memberId": number }`
- 오류: `400 BAD_REQUEST` 빈 값·형식 오류 · `409 DUPLICATE_LOGIN_ID` 아이디 중복 · `409 DUPLICATE_EMAIL` 이메일 중복 (동시 가입으로 DB에서 막혀도 같은 응답)

### `GET /api/members/check-id` 아이디 중복확인
가입 전 안내용입니다. `true`여도 가입 순간에 다른 사람이 먼저 가져가면 가입이 `409 DUPLICATE_LOGIN_ID`로 실패할 수 있습니다.
- 경로·쿼리: `loginId` string 필수 (영문·숫자 4~20자)
- 성공: `200` `{ "available": boolean }` 가입 가능하면 `true`
- 오류: `400 BAD_REQUEST` `loginId` 없음·형식 오류

## PerformanceController

### `GET /api/performances` 공연 목록
승인됐고 공연 일시가 지나지 않은 공연만, 공연 일시가 빠른 순으로 한 페이지에 10개씩 줍니다.
- 경로·쿼리: `genre` string 선택(없거나 비면 전체, 예: `CONCERT`) · `page` number 선택(기본 1)
- 성공: `200` `{ performanceList: Performance[], totalCount: number }`. totalCount는 이 장르 전체 개수(페이지 수 = ceil(totalCount / 10)). 마지막 페이지를 넘기면 빈 배열
- 오류: `400 BAD_REQUEST` page가 숫자가 아니거나 1보다 작을 때

### `POST /api/performances` 공연 등록
공연 등록(M9)과 좌석 배치(M20)를 **한 번의 요청, 한 트랜잭션**으로 저장합니다. 공연은 심사중(`P`) 상태로 생성되고, 중간에 실패하면 DB는 롤백되고 이미 저장된 파일도 삭제됩니다. 좌석 상태는 항상 `AVAILABLE`로 서버가 고정합니다.
- 요청: `multipart/form-data` (파트 3개)
  - `data` 필수 · 공연 정보 JSON, **파트 Content-Type을 `application/json`으로 지정** · [PerformanceRegisterRequest](#performanceregisterrequest)
  - `proof` 필수 · 대관 증빙 · pdf/jpg/jpeg/png · 10MB 이하
  - `poster` 선택 · 포스터 · jpg/jpeg/png/webp · 5MB 이하
- 성공: `201` `{ "performanceId": number }`
- 오류: `401 AUTH_REQUIRED` 로그인 안 함 · `400 BAD_REQUEST` 필수 파트(`data`, `proof`) 누락, 입력값 검증 실패(message에 사유), 허용되지 않는 파일 형식 · `413 FILE_TOO_LARGE` 증빙 10MB / 포스터 5MB 초과, 요청 전체 20MB 초과 · `415 UNSUPPORTED_MEDIA_TYPE` multipart가 아닌 요청 또는 `data` 파트가 JSON이 아님

---

## 응답 객체

### LoginMember
```ts
{
  memberId: number
  loginId: string
  memberName: string      // 중복 가능, 표시용
  role: "USER" | "ADMIN"
  admin: boolean          // role이 ADMIN이면 true
}
```

### Performance
날짜는 Jackson 기본값대로 epoch 밀리초 숫자입니다 (`new Date(showAt)`).
```ts
{
  performanceId: number
  registrantId: number         // 공연 등록자 MEMBER_ID
  managerId: number | null     // 심사한 관리자
  venueTemplateId: number | null
  performanceName: string
  genre: string                // 예: "CONCERT", "MUSICAL"
  castInfo: string
  description: string
  posterUrl: string | null
  runningTime: number          // 분
  showAt: number               // 공연 일시
  bookingOpenAt: number        // 예매 오픈 시각
  cancelDeadlineAt: number     // 취소 가능 기한
  maxTicketsPerMember: number
  venueName: string
  venueAddress: string
  approvalStatus: "P" | "A" | "R"   // 심사중 · 승인 · 반려
  approved: boolean            // approvalStatus가 "A"면 true
  rejectReason: string | null
  reviewedAt: number | null
  createdAt: number
}
```

## 요청 객체

### PerformanceRegisterRequest
```ts
{
  venueTemplateId: number | null   // 템플릿 공연장을 불러왔으면 venue id, 직접 그렸으면 null
  performanceName: string          // 1~200자
  genre: string                    // 필수
  castInfo: string
  description: string
  runningTime: number              // 분, 1~9999
  maxTicketsPerMember: number      // 1~99
  showAt: string                   // 공연 일시. ISO 8601 + 오프셋, 예: "2026-12-25T19:00:00+09:00" (오프셋 없으면 UTC로 해석됨)
  bookingOpenAt: string            // showAt과 같은 형식, showAt보다 앞서야 함
  cancelDeadlineAt: string         // showAt과 같은 형식, showAt 이전(같아도 됨)
  venueName: string                // 필수, 100자 이하
  venueAddress: string | null      // 255자 이하
  grades: [                        // 1개 이상
    {
      gradeKey: string             // 요청 안에서만 쓰는 임시 키, 등급마다 고유
      gradeName: string            // 1~20자
      price: number                // 0~99,999,999
    }
  ]
  rental: {
    rentalStartAt: string          // showAt과 같은 형식, 대관 기간이 showAt을 포함해야 함
    rentalEndAt: string            // showAt과 같은 형식
  }
  layout: {
    seatLayoutName: string         // 1~50자
    sourceLayoutId: number | null  // 템플릿에서 시작했으면 원본 layout id (이때 venueTemplateId 필수)
    shareAsTemplate: boolean       // 이 배치를 템플릿으로 공유할지
    sectors: [                     // 1개 이상
      {
        sectorName: string         // 1~50자
        gradeKey: string           // grades의 gradeKey 중 하나 (없으면 400)
        seats: [                   // 1개 이상
          {
            rowNum: string         // 1~10자
            columnNum: number
            seatNumber: string     // 1~20자
          }
        ]
      }
    ]
  }
}
```
- 같은 배치 안에서 `(rowNum, columnNum)`은 구역이 달라도 중복될 수 없습니다.
- 등급(`grades`)을 먼저 정의해야 구역이 `gradeKey`로 참조할 수 있습니다.

---

## 작성 틀

````markdown
### `METHOD /api/...` 기능 이름
(필요하면 한 줄 설명)
- 경로·쿼리: `performanceId` number 필수 · `page` number 선택(기본 1)
- 요청: `{ "필드": 타입 }` 또는 없음
- 성공: `200` 응답 설명 또는 [객체](#응답-객체) 링크
- 오류: `상태 CODE` 언제 · `상태 CODE` 언제
````

여러 API가 같이 쓰는 응답 모양은 [응답 객체](#응답-객체)에 한 번만 씁니다.

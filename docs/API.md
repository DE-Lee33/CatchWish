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

## 목록

| ID | Method | URI | 설명 | 로그인 | 담당 |
|---|---|---|---|---|---|
| M2 | POST | `/api/auth/login` | 로그인 | - | 현지 |
| M2 | POST | `/api/auth/logout` | 로그아웃 | - | 현지 |
| M2 | GET | `/api/members/me` | 내 로그인 정보 | 로그인 | 현지 |
| M4 | GET | `/api/performances` | 공연 목록 (장르, 페이지) | - | 현지 |

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

## PerformanceController

### `GET /api/performances` 공연 목록
승인됐고 공연 일시가 지나지 않은 공연만, 공연 일시가 빠른 순으로 한 페이지에 10개씩 줍니다.
- 경로·쿼리: `genre` string 선택(없거나 비면 전체, 예: `CONCERT`) · `page` number 선택(기본 1)
- 성공: `200` `{ performanceList: Performance[], totalCount: number }`. totalCount는 이 장르 전체 개수(페이지 수 = ceil(totalCount / 10)). 마지막 페이지를 넘기면 빈 배열
- 오류: `400 BAD_REQUEST` page가 숫자가 아니거나 1보다 작을 때

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

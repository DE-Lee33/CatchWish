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
| M1 | POST | `/api/members` | 회원가입 | - | 현지 |
| M1 | GET | `/api/members/check-id` | 아이디 중복확인 | - | 현지 |
| M22 | POST | /api/bookings/hold | 좌석 선점 및 예매 생성 | 로그인 | 재원 |

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

## BookingController

### `POST /api/bookings/hold` 좌석 선점 및 예매 생성

로그인한 회원이 선택한 공연의 좌석을 선점하고 예매 정보를 생성합니다. 선점된 좌석은 약 10분 동안 유지되며, 만료되면 자동으로 예매가 취소되고 좌석이 복원됩니다.

- 요청: `{ "performanceId": number, "seatIds": number[] }`
  - `performanceId`: 공연 ID (필수)
  - `seatIds`: 선점할 좌석 ID 목록 (필수, 중복 불가)
- 성공: `200` [BookingHoldResponse](#bookingholdresponse)
- 오류:
  - `401 AUTH_REQUIRED` 로그인하지 않음
  - `400 BAD_REQUEST` 공연 ID 누락 · 좌석 목록 누락 또는 비어 있음 · 중복 좌석 요청
  - `404 NOT_FOUND` 존재하지 않는 공연 · 해당 공연에 속하지 않는 좌석
  - `409 SEAT_ALREADY_HELD` 다른 회원이 이미 선점한 좌석
  - `400 MAX_TICKETS_EXCEEDED` 최대 예매 가능 매수 초과

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

### BookingHoldResponse

```ts
{
  bookingId: number
  performanceId: number
  seatIds: number[]
  holdExpiresAt: number    // 만료 시각 (Unix epoch milliseconds)
  remainingSeconds: number // 남은 선점 시간 (초)
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

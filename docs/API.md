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
| M12 | POST | `/api/wishlist` | 위시리스트 등록 | 로그인 | 다은 |
| M13 | GET | `/api/wishlist` | 위시리스트 조회 | 로그인 | 다은 |
| M13 | DELETE | `/api/wishlist/{id}` | 위시리스트 삭제 | 로그인 | 다은 |
| M13 | PUT | `/api/wishlist/{id}/priority` | 우선순위 변경 | 로그인 | 다은 |
| M13 | PUT | `/api/wishlist/order` | 우선순위 일괄 변경 | 로그인 | 다은 |

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

## WishlistController

### `POST /api/wishlist` 위시리스트 등록
같은 공연·구역 조합은 한 번만 등록할 수 있습니다.
- 요청: `{ "performanceId": number, "sectorId": number | null, "priority": number | null }` performanceId 필수 · sectorId, priority 선택 · priority는 1 이상
- 성공: `201` [WishlistItem](#wishlistitem)
- 오류: `400 BAD_REQUEST` 공연 없음·우선순위 1 미만 · `404 NOT_FOUND` 없는 공연·구역, 또는 해당 공연의 구역이 아님 · `409 CONFLICT` 이미 등록된 공연·구역(동시 요청 포함)

### `GET /api/wishlist` 위시리스트 조회
우선순위 오름차순(우선순위 없는 항목은 뒤), 같으면 최근 등록순입니다.
- 요청: 없음
- 성공: `200` [WishlistItem](#wishlistitem) 배열 (0건이면 빈 배열)

### `DELETE /api/wishlist/{id}` 위시리스트 삭제
- 경로·쿼리: `id` number 필수 (위시리스트 ID)
- 성공: `204`
- 오류: `404 NOT_FOUND` 없는 항목 또는 본인 항목 아님

### `PUT /api/wishlist/{id}/priority` 우선순위 변경
- 경로·쿼리: `id` number 필수 (위시리스트 ID)
- 요청: `{ "priority": number }` 필수 · 1 이상
- 성공: `200` 변경 후 [WishlistItem](#wishlistitem) 배열
- 오류: `400 BAD_REQUEST` 우선순위 없음·1 미만 · `404 NOT_FOUND` 없는 항목 또는 본인 항목 아님

### `PUT /api/wishlist/order` 우선순위 일괄 변경
화면에 보이는 순서대로 내 위시리스트 ID 전체를 보내면 1, 2, 3... 으로 다시 매깁니다. 한 트랜잭션으로 처리됩니다.
- 요청: `{ "wishlistIds": number[] }` 필수 · 내 위시리스트 ID 전체, 중복 불가
- 성공: `200` 변경 후 [WishlistItem](#wishlistitem) 배열
- 오류: `400 BAD_REQUEST` 빈 목록·중복 ID · `404 NOT_FOUND` 항목 없음 · `409 CONFLICT` 보낸 ID 목록이 현재 내 위시리스트와 다름(새로고침 필요)

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

### WishlistItem
```ts
{
  wishlist: {
    wishlistId: number
    memberId: number
    performanceId: number
    sectorId: number | null
    priority: number | null
    createdAt: number       // 밀리초 타임스탬프
  }
  performanceTitle: string
  performanceDate: number | null   // 밀리초 타임스탬프
  sectorName: string | null
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

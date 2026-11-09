# CatchWish (캐시위시)

> "누구나 열 수 있는" 실시간 공연 티켓팅 플랫폼

2026-2학기 데이터베이스프로그래밍(02분반) 3팀 프로젝트 · 2026.09.10 ~ 2026.12.16

## 프로젝트 개요

누구나 공연을 등록하고, 관리자 심사를 거쳐 실시간으로 티켓을 판매할 수 있는 플랫폼입니다.

**핵심 기능**

- **실시간 선점형 예매** — 좌석을 고르는 순간 선점하고, 결제 대기 시간 안에 결제하면 확정
- **취소표 알림** — 취소표가 나오면 메일·인앱 알림
- **위시리스트** — 관심 공연 저장, 예매 오픈 알림과 연동
- **공연 등록 심사** — 공연 등록자가 등록한 공연을 관리자가 승인·반려

## 팀원 · 역할

| 이름 | 역할 |
|---|---|
| 김현지 | 로그인·회원관리, 공연 정보 관리(공연 등록·수정·삭제, 관리자 심사), 좌석 배치도 프론트(공동) |
| 엄재원 | 티켓팅 백엔드(좌석 선점·결제), 좌석 구조 데이터 설계, 대형 공연장 템플릿 |
| 이다은 (팀장) | 티켓팅 백엔드(예매 내역·취소, 대기열), 위시리스트 |
| 황지우 | 취소표 알림(메일·인앱), 선예매, 좌석 배치도 프론트(공동) |

## 기술 스택

| 구분 | 기술 |
|---|---|
| Frontend | React (Vite) |
| Backend | Java 21, Spring MVC 6.2 (Controller 단계에서 추가 예정) |
| DB | Oracle 12c |
| DB 접근 | JDBC (수업 예제 JDBCUtil) |
| WAS | Tomcat 10.1 (Servlet 6.0, `jakarta.*`) |
| 빌드 | Maven (war) |
| IDE | Eclipse (Enterprise Java) |
| 협업 | GitHub, Notion |

## 구조

3계층 + MVC

```
React  →  Controller  →  Manager  →  DAO  →  Oracle
(화면)    (요청·응답)    (비즈니스 로직) (SQL)
```

## 폴더 구조

```
CatchWish/
├─ backend/                  Eclipse Maven 프로젝트
│  ├─ pom.xml
│  └─ src/
│     ├─ main/java/
│     │  ├─ model/domain/    도메인 클래스 (Member, Performance ...)
│     │  ├─ model/dto/       화면·조회용 DTO
│     │  ├─ model/dao/       JDBCUtil, ConnectionManager, 각자 DAO
│     │  ├─ model/manager/   비즈니스 로직
│     │  └─ controller/      Controller (Spring MVC 추가 후)
│     ├─ main/resources/     db.properties (깃에 안 올림), db.properties.example
│     ├─ main/webapp/        web.xml 등 (Controller 단계)
│     └─ test/java/          DAO 테스트
├─ frontend/                 React (Vite, 나중에)
├─ db/                       DB 사용 규칙, 스키마 변경 기록(migrations)
├─ docs/                     협업 규칙, 상태값 코드표
└─ .github/                  PR 템플릿
```

## 처음 시작하기

1. **설치:** JDK 21, Tomcat 10.1, Eclipse (Enterprise Java)
2. **Eclipse 인코딩:** Window > Preferences > General > Workspace > Text file encoding → `UTF-8`
3. **clone**
   ```bash
   git clone https://github.com/DE-Lee33/CatchWish.git
   ```
4. **Import:** Eclipse > File > Import > Maven > Existing Maven Projects → `backend` 폴더 선택
5. **DB 접속 설정:** `backend/src/main/resources/db.properties.example`을 같은 폴더에 `db.properties`로 복사하고 노션에 있는 팀 계정 정보 입력
   - `db.properties`는 `.gitignore`에 등록되어 있어 커밋되지 않습니다. **계정 정보를 다른 파일에 적지 마세요**
6. **연결 확인:** `backend/src/test/java/model/dao/ConnectionTest.java` 우클릭 > Run As > JUnit Test → 초록색이면 성공
   - `db.properties`가 없으면 테스트가 건너뛰어집니다
7. **작업 시작:** `dev` 브랜치에서 시작 (자세한 흐름은 [CONVENTIONS.md](docs/CONVENTIONS.md))
   ```bash
   git checkout dev
   ```

## 문서

- 노션: [DBP-Project](https://app.notion.com/p/DBP-Project-a931987843da82a6bb9c0141fa42d34f)
- [docs/CONVENTIONS.md](docs/CONVENTIONS.md) — 브랜치·커밋·PR·코드 규칙
- [docs/STATUS_CODES.md](docs/STATUS_CODES.md) — 상태값 코드표
- [db/README.md](db/README.md) — DB 사용 규칙, 스키마 변경 기록 방법

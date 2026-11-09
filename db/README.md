# db

팀 공용 Oracle 12c 계정 하나를 4명이 같이 씁니다. 접속 정보는 노션에만 있고 깃에는 올리지 않습니다.

## 사용 규칙

- DROP · 테이블 재생성은 **단톡 공지 후에만**
- 다른 사람 테이블의 구조는 담당자와 먼저 이야기하고 바꾸기
- `migrations/`의 파일은 이미 DB에 반영된 기록입니다. **다시 실행하지 마세요**

## 스키마 변경 기록 방법

1. SQL Developer에서 변경 SQL 실행 (팀 공용 DB에 반영)
2. 실행한 SQL을 `migrations/YYYY-MM-DD_대상_내용.sql`로 저장
   - 맨 위에 `-- ⚠️ 이미 팀 공용 DB에 반영됨. 재실행 금지` 주석
   - 예) `2026-11-09_performance_add_columns.sql`
3. 노션 [Database 설계](https://app.notion.com/p/3fb1987843da809cae9fd9986856d74a) 페이지 수정
4. 단톡에 공지
5. 상태값 코드를 추가·변경했다면 [docs/STATUS_CODES.md](../docs/STATUS_CODES.md)도 갱신

## 기록 목록

| 파일 | 내용 |
|---|---|
| `2026-11-09_performance_add_columns.sql` | PERFORMANCE 공연 정보·심사 컬럼, CHECK 제약 추가 |
| `2026-11-09_notification_presale.sql` | NOTIFICATION 수신 회원·읽음 여부, PRESALE_CODE 생성, CHECK_AUTH_CODE FK, 시퀀스 |

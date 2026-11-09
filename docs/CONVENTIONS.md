# 협업 규칙

기준 문서: 노션 [개발 환경 · 프로젝트 구조 · 협업 규칙](https://app.notion.com/p/4131987843da803bb063c0e4a7ac9ef5). 바뀌면 두 곳 모두 갱신합니다.

## 1. 브랜치

| 브랜치 | 용도 |
|---|---|
| `main` | 최종본. 제출·발표 때만 merge |
| `dev` | 팀 작업을 모으는 기본 브랜치 |
| `feat/기능명` | 각자 작업. 예) `feat/member-dao` |

- **`main`, `dev` 직접 push 금지.** PR로만 merge, 팀원 1명 이상 승인 필요

### 작업 흐름

1. `dev`를 pull
2. `dev`에서 `feat/기능명` 브랜치 생성
3. 작업 및 커밋
4. push 후 `dev`로 PR
5. 팀원 1명 이상 승인 후 merge
6. merge된 feature 브랜치 삭제

작업 중 `dev`가 바뀌었으면, 내 브랜치에 `dev`를 먼저 merge하고 충돌을 해결한 뒤 PR을 올립니다.

```bash
git checkout dev
git pull
git checkout -b feat/member-dao
# ... 작업, 커밋 ...
git push -u origin feat/member-dao
```

## 2. 커밋 메시지

형식: `종류: 내용` 예) `feat: 회원 DAO 구현`

| 종류 | 의미 |
|---|---|
| feat | 새 기능 |
| fix | 버그 수정 |
| docs | 문서 |
| refactor | 동작 변경 없는 코드 정리 |
| chore | 빌드·설정 (pom.xml, .gitignore 등) |

## 3. 이름 규칙

- 클래스: PascalCase (`MemberDAO`)
- 메서드·변수: camelCase (`findById`)
- DB 테이블·컬럼: UPPER_SNAKE (`PERFORMANCE_NAME`)
- 회원은 `member` (`user` 사용 X), "주최자" 대신 "공연 등록자"

## 4. DAO 규칙

- 테이블(또는 묶음)당 DAO 1개, 위치는 `model.dao`
- SQL 값은 반드시 `?` 바인딩 (문자열 이어붙이기 금지)
- JDBCUtil 사용 순서
  1. `setSqlAndParameters(sql, new Object[] {...})`
  2. 조회: `executeQuery()` / 변경: `executeUpdate()`
  3. 변경 후 `commit()`, 예외 시 `rollback()`
  4. `finally`에서 `close()`
- JDBCUtil은 `autoCommit=false`라서 **변경 후 `commit()`을 안 하면 반영되지 않습니다**
- 날짜 값은 `java.sql.Timestamp`로 변환해서 전달
- 상태값은 [STATUS_CODES.md](STATUS_CODES.md)의 1글자 코드 사용

```java
JDBCUtil jdbcUtil = new JDBCUtil();
try {
    jdbcUtil.setSqlAndParameters("UPDATE MEMBER SET EMAIL = ? WHERE MEMBER_ID = ?",
            new Object[] { email, memberId });
    int result = jdbcUtil.executeUpdate();
    jdbcUtil.commit();
    return result;
} catch (Exception ex) {
    jdbcUtil.rollback();
    throw ex;
} finally {
    jdbcUtil.close();
}
```

## 5. DB 규칙

- 팀 공용 계정이라 DROP · 테이블 재생성은 **단톡 공지 후에만**
- 스키마를 바꾸면: SQL Developer 반영 → `db/migrations`에 기록 → 노션 Database 설계 수정 → 단톡 공지 (자세한 방법: [db/README.md](../db/README.md))

## 6. 금지

- DB 접속 정보(비밀번호)가 든 파일 커밋 — `db.properties`는 `.gitignore`에 등록되어 있음
- 남의 파일 수정 (필요하면 담당자에게 요청)
- `bin/`, `target/` 같은 빌드 결과물 커밋

## 7. 미정 · 검토 항목

- **JDBCUtil 동시성 (미정):** JDBCUtil의 Connection이 `static`이라 동시 요청 시 서로의 트랜잭션이 섞일 수 있습니다. 좌석 선점(M22) 구현·테스트 전에 요청별 연결로 바꿀지 결정합니다. 바꾸더라도 DAO 사용법(위 4번)은 그대로 유지하는 방향입니다.

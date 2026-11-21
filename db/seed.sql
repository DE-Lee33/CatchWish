-- =====================================================================
-- db/seed.sql : CatchWish 테스트 데이터 통합본
--   현지 = 회원·공연·등급·대관 증빙
--   다은 = 위시·대기열·예매·결제
--   재원 = 공연장·좌석 배치·좌석 선점·결제 처리
--   지우 = 취소표 재오픈 일정·알림·선예매 코드·선예매 인증
--
-- 구성 (순서를 바꾸면 FK 오류가 남)
--   PART 1. CLEANUP : 지우 -> 재원 -> 다은 -> 현지   (참조하는 쪽부터 삭제)
--   PART 2. INSERT  : 현지 -> 다은 -> 재원 -> 지우   (참조당하는 쪽부터 입력)
--   PART 3. CHECK   : 현지 -> 다은 -> 재원
--
-- 선행 관계
--   다은 INSERT = 현지의 회원(seed_user1, seed_user2, seed_host2)·공연 필요
--   재원 INSERT = 현지의 공연·등급, 다은과 같은 회원(seed_user1) 필요
--   지우 INSERT = 현지의 회원·공연, 다은의 WISHLIST 필요
-- =====================================================================


-- #####################################################################
-- PART 1. CLEANUP  (지우 -> 재원 -> 다은 -> 현지)
-- #####################################################################

-- ---------------------------------------------------------------------
-- 1-1. 지우 cleanup
-- ---------------------------------------------------------------------
DELETE FROM CHECK_AUTH_CODE
WHERE CREATED_AT = DATE '2000-01-01'
  AND MEMBER_ID IN (
      SELECT MEMBER_ID FROM MEMBER
      WHERE LOGIN_ID = 'seed_user1'
  )
  AND PERFORMANCE_ID IN (
      SELECT PERFORMANCE_ID FROM PERFORMANCE
      WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌'
  );

DELETE FROM NOTIFICATION
WHERE CANCELED_TICKET_SCHEDULE_ID IN (
    SELECT s.CANCELED_TICKET_SCHEDULE_ID
    FROM CANCELED_TICKET_SCHEDULE s
    JOIN PERFORMANCE p ON p.PERFORMANCE_ID = s.PERFORMANCE_ID
    WHERE p.PERFORMANCE_NAME = '2026 캐치위시 페스티벌'
      AND s.CREATED_AT = DATE '2000-01-01'
);

DELETE FROM CANCELED_TICKET_SCHEDULE
WHERE CREATED_AT = DATE '2000-01-01'
  AND PERFORMANCE_ID IN (
      SELECT PERFORMANCE_ID
      FROM PERFORMANCE
      WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌'
  );

DELETE FROM PRESALE_CODE
WHERE CODE_VALUE IN ('TESTCODE001', 'TESTCODE002')
  AND PERFORMANCE_ID IN (
      SELECT PERFORMANCE_ID
      FROM PERFORMANCE
      WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌'
  );

-- ---------------------------------------------------------------------
-- 1-2. 재원 cleanup (예매/결제 -> 좌석 -> 구역 -> 배치도)
-- ---------------------------------------------------------------------
-- 이 파일이 만든 예매/결제 삭제 (FK를 끄지 않음)
-- 순서: 좌석 연결 삭제 + BOOKING.PAYMENT_ID 비우기 -> PAYMENT 삭제 -> BOOKING 삭제
-- 결제 삭제 후에는 예매를 찾을 수 없으므로 예매 번호를 먼저 모아 둠
DECLARE
    TYPE t_ids IS TABLE OF NUMBER;
    v_bids t_ids;
BEGIN
    SELECT BOOKING_ID BULK COLLECT INTO v_bids
    FROM PAYMENT
    WHERE TRANSACTION_ID LIKE 'DAOTEST%';

    FOR i IN 1 .. v_bids.COUNT LOOP
        DELETE FROM BOOKING_SEAT WHERE BOOKING_ID = v_bids(i);
        UPDATE BOOKING SET PAYMENT_ID = NULL WHERE BOOKING_ID = v_bids(i);
    END LOOP;

    DELETE FROM PAYMENT
    WHERE TRANSACTION_ID LIKE 'DAOTEST%';

    FOR i IN 1 .. v_bids.COUNT LOOP
        DELETE FROM BOOKING WHERE BOOKING_ID = v_bids(i);
    END LOOP;

    COMMIT;
END;
/

-- 이전 DAO 테스트 배치 삭제 (BOOKING_SEAT가 연결된 배치는 삭제 제외, 공연장은 재사용)
DELETE FROM SEAT
WHERE SEAT_LAYOUT_ID IN (
    SELECT SL.SEAT_LAYOUT_ID
    FROM SEAT_LAYOUT SL
    WHERE (SL.SEAT_LAYOUT_NAME LIKE 'DAO - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO TPL - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO 테스트%')
      AND NOT EXISTS (
          SELECT 1
          FROM SEAT S2
          JOIN BOOKING_SEAT BS ON BS.SEAT_ID = S2.SEAT_ID
          WHERE S2.SEAT_LAYOUT_ID = SL.SEAT_LAYOUT_ID
      )
);

DELETE FROM SECTOR
WHERE SEAT_LAYOUT_ID IN (
    SELECT SL.SEAT_LAYOUT_ID
    FROM SEAT_LAYOUT SL
    WHERE (SL.SEAT_LAYOUT_NAME LIKE 'DAO - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO TPL - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO 테스트%')
      AND NOT EXISTS (
          SELECT 1
          FROM SEAT S2
          JOIN BOOKING_SEAT BS ON BS.SEAT_ID = S2.SEAT_ID
          WHERE S2.SEAT_LAYOUT_ID = SL.SEAT_LAYOUT_ID
      )
);

DELETE FROM SEAT_LAYOUT SL
WHERE (SL.SEAT_LAYOUT_NAME LIKE 'DAO - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO TPL - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO 테스트%')
  AND SL.IS_TEMPLATE = 'N'
  AND NOT EXISTS (
      SELECT 1
      FROM SEAT S2
      JOIN BOOKING_SEAT BS ON BS.SEAT_ID = S2.SEAT_ID
      WHERE S2.SEAT_LAYOUT_ID = SL.SEAT_LAYOUT_ID
  );

DELETE FROM SEAT_LAYOUT SL
WHERE (SL.SEAT_LAYOUT_NAME LIKE 'DAO - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO TPL - %' OR SL.SEAT_LAYOUT_NAME LIKE 'DAO 테스트%')
  AND SL.IS_TEMPLATE = 'Y'
  AND NOT EXISTS (
      SELECT 1 FROM SEAT_LAYOUT CH WHERE CH.SOURCE_LAYOUT_ID = SL.SEAT_LAYOUT_ID
  );

-- ---------------------------------------------------------------------
-- 1-3. 다은 cleanup (seed_ 회원의 예매·결제·대기열·위시)
-- ---------------------------------------------------------------------
UPDATE BOOKING SET PAYMENT_ID = NULL
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\');

DELETE FROM PAYMENT
WHERE BOOKING_ID IN (SELECT BOOKING_ID FROM BOOKING
                     WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\'));

DELETE FROM BOOKING
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\');

DELETE FROM QUEUE
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\');

DELETE FROM WISHLIST
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\');

COMMIT;

-- ---------------------------------------------------------------------
-- 1-4. 현지 cleanup (기존 seed 삭제: 회원·공연·등급·증빙) - 반드시 마지막
-- ---------------------------------------------------------------------
UPDATE PERFORMANCE SET MANAGER_ID = NULL
WHERE MANAGER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\');

DELETE FROM VENUE_RENTAL_PROOF
WHERE PERFORMANCE_ID IN (SELECT PERFORMANCE_ID FROM PERFORMANCE
                         WHERE REGISTRANT_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\'));

DELETE FROM SEAT_GRADE
WHERE PERFORMANCE_ID IN (SELECT PERFORMANCE_ID FROM PERFORMANCE
                         WHERE REGISTRANT_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\'));

DELETE FROM PERFORMANCE
WHERE REGISTRANT_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\');

DELETE FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\';

COMMIT;


-- #####################################################################
-- PART 2. INSERT  (현지 -> 다은 -> 재원 -> 지우)
-- #####################################################################

-- ---------------------------------------------------------------------
-- 2-1. 현지: 회원·공연 / MEMBER, PERFORMANCE, SEAT_GRADE, VENUE_RENTAL_PROOF
-- 의도: 회원(로그인·중복확인·등록 공연 0개 회원), 공연(장르·페이징 8개 기준·'별빛' 이름 검색·'한서윤' 출연진 검색·예매 오픈 전후·취소기한 지남·지난 공연), 심사(승인·심사중·반려, 대관 기간 불일치 1건)
-- 예상: 회원 6명 해시 모두 Y / 승인 CONCERT 9·MUSICAL 3, 심사중 3, 반려 1 / 공연마다 등급 1~4개·증빙 1개 / 대관 기간 불일치는 '뮤지컬 마지막 편지'만 N
-- 결과: 11/18 실행, 예상과 동일
-- ---------------------------------------------------------------------
DECLARE
    d        DATE := TRUNC(SYSDATE);
    c_test   CONSTANT VARCHAR2(64) := '937e8d5fbb48bd4949536cd65b8d35c426b80d2f830c5c308e2cdec422ae2244';
    c_admin  CONSTANT VARCHAR2(64) := 'ac9689e2272427085e35b9d3e3e8bed88cb3434828b43b86fc0596cad4c6e270';
    v_admin  NUMBER;
    v_host1  NUMBER;
    v_host2  NUMBER;
    v_host3  NUMBER;
    v_user1  NUMBER;
    v_user2  NUMBER;
    v_pid    NUMBER;

    FUNCTION add_member(p_login VARCHAR2, p_hash VARCHAR2, p_name VARCHAR2,
                        p_email VARCHAR2, p_role VARCHAR2, p_created DATE) RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        SELECT SEQ_MEMBER.NEXTVAL INTO v_id FROM DUAL;
        INSERT INTO MEMBER (MEMBER_ID, LOGIN_ID, PASSWORD_HASH, MEMBER_NAME, EMAIL, MEMBER_ROLE, CREATED_AT)
        VALUES (v_id, p_login, p_hash, p_name, p_email, p_role, p_created);
        RETURN v_id;
    END;

    FUNCTION add_perf(p_registrant NUMBER, p_name VARCHAR2, p_genre VARCHAR2, p_cast VARCHAR2,
                      p_desc VARCHAR2, p_running NUMBER, p_show DATE, p_open DATE, p_cancel DATE,
                      p_max NUMBER, p_venue VARCHAR2, p_addr VARCHAR2,
                      p_status CHAR, p_manager NUMBER, p_reason VARCHAR2, p_reviewed DATE,
                      p_created DATE) RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        SELECT SEQ_PERFORMANCE.NEXTVAL INTO v_id FROM DUAL;
        INSERT INTO PERFORMANCE (PERFORMANCE_ID, REGISTRANT_ID, VENUE_TEMPLATE_ID, MANAGER_ID,
               PERFORMANCE_NAME, GENRE, CAST_INFO, DESCRIPTION, POSTER_URL, RUNNING_TIME,
               SHOW_AT, BOOKING_OPEN_AT, CANCEL_DEADLINE_AT, MAX_TICKETS_PER_MEMBER,
               VENUE_NAME, VENUE_ADDRESS, APPROVAL_STATUS, REJECT_REASON, REVIEWED_AT, CREATED_AT)
        VALUES (v_id, p_registrant, NULL, p_manager,
               p_name, p_genre, p_cast, p_desc, NULL, p_running,
               p_show, p_open, p_cancel, p_max,
               p_venue, p_addr, p_status, p_reason, p_reviewed, p_created);
        RETURN v_id;
    END;

    PROCEDURE add_grade(p_pid NUMBER, p_name VARCHAR2, p_price NUMBER) IS
    BEGIN
        INSERT INTO SEAT_GRADE (GRADE_ID, PERFORMANCE_ID, GRADE_NAME, PRICE)
        VALUES (SEQ_SEAT_GRADE.NEXTVAL, p_pid, p_name, p_price);
    END;

    PROCEDURE add_proof(p_pid NUMBER, p_file VARCHAR2, p_orig VARCHAR2,
                        p_start DATE, p_end DATE, p_submitted DATE) IS
    BEGIN
        INSERT INTO VENUE_RENTAL_PROOF (PROOF_ID, PERFORMANCE_ID, FILE_PATH, ORIGINAL_FILE_NAME,
               RENTAL_START_AT, RENTAL_END_AT, SUBMITTED_AT)
        VALUES (SEQ_RENTAL_PROOF.NEXTVAL, p_pid, p_file, p_orig, p_start, p_end, p_submitted);
    END;

BEGIN
    --member (관리자 1, 일반 5)
    v_admin := add_member('seed_admin', c_admin, '캐치위시관리자', 'seed_admin@example.com', 'ADMIN', d - 60);
    v_host1 := add_member('seed_host1', c_test,  '정하늘',         'seed_host1@example.com', 'USER',  d - 50);
    v_host2 := add_member('seed_host2', c_test,  '오세린',         'seed_host2@example.com', 'USER',  d - 40);
    v_host3 := add_member('seed_host3', c_test,  '최도윤',         'seed_host3@example.com', 'USER',  d - 50);
    v_user1 := add_member('seed_user1', c_test,  '한유진',         'seed_user1@example.com', 'USER',  d - 30);
    v_user2 := add_member('seed_user2', c_test,  '서민재',         'seed_user2@example.com', 'USER',  d - 20);

    --performance (+ seat_grade, venue_rental_proof)
    v_pid := add_perf(v_host1, '2026 캐치위시 페스티벌', 'CONCERT', '밴드 루멘, 소울트레인, DJ 하루',
                      '캐치위시가 처음 여는 연말 페스티벌. 밴드와 DJ 공연이 이어집니다.', 180,
                      d + 30 + 18/24, d - 7 + 20/24, d + 29 + 18/24, 4,
                      '캐치위시 아레나', '서울특별시 가상구 공연로 1',
                      'A', v_admin, NULL, d - 9, d - 12);
    add_grade(v_pid, 'VIP', 165000);
    add_grade(v_pid, 'R',   143000);
    add_grade(v_pid, 'S',   121000);
    add_grade(v_pid, 'A',    99000);
    add_proof(v_pid, '/upload/proof/seed_c1.pdf', '대관확인서_캐치위시아레나.pdf',
              d + 30 + 8/24, d + 31 + 2/24, d - 12);

    v_pid := add_perf(v_host1, '인디 밴드 연합 공연 Vol.3', 'CONCERT', '파란새벽, 모래시계, 늦은오후',
                      '신인 인디 밴드 세 팀의 합동 공연.', 120,
                      d + 21 + 19/24, d + 2 + 20/24, d + 20 + 19/24, 4,
                      '홍대 라이브홀 웨이브', '서울특별시 가상구 음악로 22',
                      'A', v_admin, NULL, d - 4, d - 8);
    add_grade(v_pid, '지정석', 77000);
    add_grade(v_pid, '스탠딩', 66000);
    add_proof(v_pid, '/upload/proof/seed_c4.pdf', '대관확인서_라이브홀웨이브.pdf',
              d + 21 + 9/24, d + 22 + 3/24, d - 8);

    v_pid := add_perf(v_host1, '봄날의 별빛 콘서트', 'CONCERT', '윤슬',
                      '봄을 기다리는 어쿠스틱 콘서트.', 100,
                      d + 40 + 19/24, d + 20 + 20/24, d + 39 + 19/24, 4,
                      '어쿠스틱 카페 홀 결', '서울특별시 가상구 노래길 7',
                      'P', NULL, NULL, NULL, d - 3);
    add_grade(v_pid, 'R', 88000);
    add_grade(v_pid, 'S', 66000);
    add_proof(v_pid, '/upload/proof/seed_p1.pdf', '대관확인서_카페홀결.pdf',
              d + 40 + 9/24, d + 41 + 3/24, d - 3);

    v_pid := add_perf(v_host1, '뮤지컬 마지막 편지', 'MUSICAL', '서지안, 문태경',
                      '전쟁 중에 쓰인 편지를 따라가는 2인 뮤지컬.', 140,
                      d + 33 + 19/24, d + 15 + 14/24, d + 32 + 19/24, 4,
                      '캐치위시 아트센터 소극장', '서울특별시 가상구 무대로 3',
                      'P', NULL, NULL, NULL, d - 2);
    add_grade(v_pid, 'R', 99000);
    add_grade(v_pid, 'S', 77000);
    add_proof(v_pid, '/upload/proof/seed_p2.pdf', '대관확인서_아트센터소극장.pdf',
              d + 31 + 9/24, d + 31 + 23/24, d - 2);

    v_pid := add_perf(v_host1, '미완성 교향곡 콘서트', 'CONCERT', '캐치위시 체임버 오케스트라',
                      '학생 오케스트라의 정기 연주회.', 110,
                      d + 25 + 19.5/24, d + 10 + 20/24, d + 24 + 19/24, 4,
                      '가상대학교 대강당', '서울특별시 가상구 학교로 10',
                      'R', v_admin, '대관 증빙 서류의 공연장 이름이 등록 정보와 다릅니다.', d - 4, d - 6);
    add_grade(v_pid, 'R', 30000);
    add_grade(v_pid, 'S', 20000);
    add_proof(v_pid, '/upload/proof/seed_r1.pdf', '대관확인서_대강당.pdf',
              d + 25 + 9/24, d + 26 + 3/24, d - 6);

    v_pid := add_perf(v_host3, '별빛 아래 어쿠스틱 나이트', 'CONCERT', '윤슬, 바람소리 트리오',
                      '작은 홀에서 듣는 어쿠스틱 라이브.', 100,
                      d + 14 + 19.5/24, d - 3 + 20/24, d + 13 + 19/24, 2,
                      '어쿠스틱 카페 홀 결', '서울특별시 가상구 노래길 7',
                      'A', v_admin, NULL, d - 13, d - 15);
    add_grade(v_pid, 'R', 55000);
    add_grade(v_pid, 'S', 44000);
    add_proof(v_pid, '/upload/proof/seed_c2.pdf', '대관확인서_카페홀결_2.pdf',
              d + 14 + 9/24, d + 15 + 3/24, d - 15);

    v_pid := add_perf(v_host3, '윈터 라이브 투어 - 서울', 'CONCERT', '한서윤, 밴드 오로라',
                      '겨울 전국 투어의 서울 공연.', 150,
                      d + 18 + 18/24, d - 5 + 20/24, d + 17 + 18/24, 4,
                      '한강 라이브센터', '서울특별시 가상구 강변로 5',
                      'A', v_admin, NULL, d - 12, d - 14);
    add_grade(v_pid, 'VIP', 132000);
    add_grade(v_pid, 'R',   110000);
    add_grade(v_pid, 'S',    88000);
    add_proof(v_pid, '/upload/proof/seed_c3.pdf', '대관확인서_한강라이브센터.pdf',
              d + 18 + 8/24, d + 19 + 2/24, d - 14);

    v_pid := add_perf(v_host3, '재즈 인 더 시티', 'CONCERT', '모던 재즈 쿼텟',
                      '도심 속 재즈 클럽 공연.', 120,
                      d + 45 + 20/24, d - 1 + 20/24, d + 44 + 20/24, 4,
                      '재즈클럽 세븐스', '서울특별시 가상구 재즈길 77',
                      'A', v_admin, NULL, d - 11, d - 13);
    add_grade(v_pid, 'R', 77000);
    add_grade(v_pid, 'S', 55000);
    add_proof(v_pid, '/upload/proof/seed_c5.pdf', '대관확인서_재즈클럽세븐스.pdf',
              d + 45 + 10/24, d + 46 + 4/24, d - 13);

    v_pid := add_perf(v_host3, '락 페스티벌 리턴즈', 'CONCERT', '크림슨 노이즈, 블랙 스완',
                      '하루 종일 이어지는 락 페스티벌.', 360,
                      d + 60 + 15/24, d - 10 + 20/24, d + 59 + 15/24, 4,
                      '캐치위시 아레나', '서울특별시 가상구 공연로 1',
                      'A', v_admin, NULL, d - 18, d - 20);
    add_grade(v_pid, '지정석', 132000);
    add_grade(v_pid, '스탠딩', 110000);
    add_proof(v_pid, '/upload/proof/seed_c6.pdf', '대관확인서_캐치위시아레나_2.pdf',
              d + 60 + 5/24, d + 60 + 23/24, d - 20);

    v_pid := add_perf(v_host3, '클래식 갈라 콘서트', 'CONCERT', '캐치위시 필하모닉',
                      '연말 클래식 명곡 모음.', 120,
                      d + 2 + 19.5/24, d - 20 + 14/24, d - 1 + 18/24, 4,
                      '캐치위시 콘서트홀', '서울특별시 가상구 음악로 100',
                      'A', v_admin, NULL, d - 28, d - 30);
    add_grade(v_pid, 'R', 90000);
    add_grade(v_pid, 'S', 70000);
    add_grade(v_pid, 'A', 50000);
    add_proof(v_pid, '/upload/proof/seed_c7.pdf', '대관확인서_콘서트홀.pdf',
              d + 2 + 9/24, d + 3 + 3/24, d - 30);

    v_pid := add_perf(v_host3, '새해 카운트다운 콘서트', 'CONCERT', '밴드 루멘, 윤슬',
                      '자정을 함께 맞는 카운트다운 공연.', 150,
                      d + 44 + 22/24, d + 7 + 20/24, d + 43 + 22/24, 4,
                      '한강 라이브센터', '서울특별시 가상구 강변로 5',
                      'A', v_admin, NULL, d - 3, d - 5);
    add_grade(v_pid, 'VIP', 154000);
    add_grade(v_pid, 'R',   132000);
    add_proof(v_pid, '/upload/proof/seed_c8.pdf', '대관확인서_한강라이브센터_2.pdf',
              d + 44 + 12/24, d + 45 + 6/24, d - 5);

    v_pid := add_perf(v_host3, '가을 소풍 콘서트', 'CONCERT', '피크닉 밴드',
                      '공원에서 열린 야외 공연.', 90,
                      d - 10 + 16/24, d - 40 + 20/24, d - 11 + 18/24, 4,
                      '가상 숲속 야외무대', '서울특별시 가상구 공원로 9',
                      'A', v_admin, NULL, d - 43, d - 45);
    add_grade(v_pid, '지정석', 33000);
    add_grade(v_pid, '무료석', 0);
    add_proof(v_pid, '/upload/proof/seed_c9.pdf', '대관확인서_야외무대.pdf',
              d - 10 + 6/24, d - 10 + 23/24, d - 45);

    v_pid := add_perf(v_host3, '뮤지컬 별빛 정원', 'MUSICAL', '이도하, 한유나',
                      '별빛이 내리는 정원에서 벌어지는 이야기.', 160,
                      d + 20 + 19.5/24, d - 5 + 14/24, d + 19 + 19/24, 4,
                      '캐치위시 아트센터', '서울특별시 가상구 무대로 3',
                      'A', v_admin, NULL, d - 14, d - 16);
    add_grade(v_pid, 'VIP', 150000);
    add_grade(v_pid, 'R',   130000);
    add_grade(v_pid, 'S',   100000);
    add_proof(v_pid, '/upload/proof/seed_m1.pdf', '대관확인서_아트센터.pdf',
              d + 20 + 9/24, d + 21 + 3/24, d - 16);

    v_pid := add_perf(v_host3, '뮤지컬 시간의 정거장', 'MUSICAL', '서지안, 문태경',
                      '시간이 멈춘 기차역을 배경으로 한 창작 뮤지컬.', 150,
                      d + 35 + 19.5/24, d - 2 + 14/24, d + 34 + 19/24, 4,
                      '캐치위시 아트센터', '서울특별시 가상구 무대로 3',
                      'A', v_admin, NULL, d - 9, d - 11);
    add_grade(v_pid, 'R', 120000);
    add_grade(v_pid, 'S',  90000);
    add_proof(v_pid, '/upload/proof/seed_m2.pdf', '대관확인서_아트센터_2.pdf',
              d + 35 + 9/24, d + 36 + 3/24, d - 11);

    v_pid := add_perf(v_host3, '뮤지컬 고양이 탐정', 'MUSICAL', '노은채, 백시우',
                      '가족이 함께 보는 추리 뮤지컬.', 120,
                      d + 50 + 15/24, d - 1 + 14/24, d + 49 + 15/24, 6,
                      '캐치위시 아트센터 소극장', '서울특별시 가상구 무대로 3',
                      'A', v_admin, NULL, d - 5, d - 7);
    add_grade(v_pid, 'R', 88000);
    add_grade(v_pid, 'S', 66000);
    add_proof(v_pid, '/upload/proof/seed_m3.pdf', '대관확인서_아트센터소극장_2.pdf',
              d + 50 + 5/24, d + 50 + 23/24, d - 7);

    v_pid := add_perf(v_host3, '소극장 버스킹 데이', 'CONCERT', '거리의 악사들',
                      '여러 버스커가 차례로 무대에 오르는 공연.', 90,
                      d + 28 + 17/24, d + 14 + 20/24, d + 27 + 17/24, 4,
                      '홍대 라이브홀 웨이브', '서울특별시 가상구 음악로 22',
                      'P', NULL, NULL, NULL, d - 1);
    add_grade(v_pid, '자유석', 22000);
    add_proof(v_pid, '/upload/proof/seed_p3.pdf', '대관확인서_라이브홀웨이브_2.pdf',
              d + 28 + 7/24, d + 29 + 1/24, d - 1);

    COMMIT;
END;
/

-- ---------------------------------------------------------------------
-- 2-2. 다은: 위시·대기열·예매·결제
-- 선행 조건: 현지 seed(회원·공연)가 위에서 들어가 있어야 함 (seed_user1, seed_user2, seed_host2, 승인 공연 4개)
-- 의도: 위시(우선순위 정렬·회원별 목록·빈 목록), 대기열(대기 W·입장 A·만료 E, 인증번호 확인 전/후, 공연별 대기자 수),
--       예매(선점 W·확정 C·취소 X, 환불 계좌 유무, 홀드 만료 대상, 취소기한 지난 공연, 결제 연결)
-- 예상(다은 단독): WISHLIST 5건 / QUEUE 5건 (W 2, A 2, E 1) / BOOKING 6건 (W 2, C 3, X 1) / PAYMENT 4건 (예매 C 3 + X 1)
-- 예상(재원 seed 포함 후): BOOKING 9건 (W 2, C 3, X 4) / PAYMENT 7건 - 재원 seed가 seed_user1 예매·결제를 3건 추가함
-- 주의: 좌석(SEAT)·BOOKING_SEAT는 재원 담당. 재원 seed가 BOOKING_SEAT를 만들므로
--       cleanup은 반드시 재원 -> 다은 순서로 실행해야 FK 오류가 나지 않음 (PART 1 순서 참고)
-- ---------------------------------------------------------------------
DECLARE
    v_user1     NUMBER;
    v_user2     NUMBER;
    v_host2     NUMBER;
    v_festival  NUMBER;  -- 2026 캐치위시 페스티벌 (예매 오픈됨, 취소기한 남음)
    v_winter    NUMBER;  -- 윈터 라이브 투어 - 서울 (예매 오픈됨, 취소기한 남음)
    v_classic   NUMBER;  -- 클래식 갈라 콘서트 (취소기한 지남)
    v_jazz      NUMBER;  -- 재즈 인 더 시티 (예매 오픈됨)
    v_bid       NUMBER;

    FUNCTION f_member(p_login VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT MEMBER_ID INTO v FROM MEMBER WHERE LOGIN_ID = p_login;
        RETURN v;
    END;

    FUNCTION f_perf(p_name VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT PERFORMANCE_ID INTO v FROM PERFORMANCE WHERE PERFORMANCE_NAME = p_name;
        RETURN v;
    END;

    PROCEDURE add_wish(p_member NUMBER, p_perf NUMBER, p_priority NUMBER) IS
    BEGIN
        INSERT INTO WISHLIST (WISHLIST_ID, SECTOR_ID, PRIORITY, CREATED_AT, MEMBER_ID, PERFORMANCE_ID)
        VALUES (SEQ_WISHLIST.NEXTVAL, NULL, p_priority, SYSDATE, p_member, p_perf);
    END;

    PROCEDURE add_queue(p_member NUMBER, p_perf NUMBER, p_pos NUMBER, p_status CHAR,
                        p_code VARCHAR2, p_verified VARCHAR2) IS
    BEGIN
        INSERT INTO QUEUE (QUEUE_ID, POSITION, STATUS, AUTH_CODE, IS_VERIFIED, CREATED_AT, MEMBER_ID, PERFORMANCE_ID)
        VALUES (SEQ_QUEUE.NEXTVAL, p_pos, p_status, p_code, p_verified, SYSDATE - 1/24, p_member, p_perf);
    END;

    -- BOOKING.PAYMENT_ID와 PAYMENT.BOOKING_ID가 서로 참조하므로 BOOKING 먼저 넣고 PAYMENT 후 연결
    FUNCTION add_booking(p_member NUMBER, p_perf NUMBER, p_status CHAR, p_refund VARCHAR2,
                         p_created DATE) RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        SELECT SEQ_BOOKING.NEXTVAL INTO v_id FROM DUAL;
        INSERT INTO BOOKING (BOOKING_ID, BOOKING_STATUS, REFUND_ACCOUNT, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID)
        VALUES (v_id, p_status, p_refund, p_created, p_member, NULL, p_perf);
        RETURN v_id;
    END;

    PROCEDURE add_payment(p_booking NUMBER, p_amount NUMBER, p_status CHAR, p_tx VARCHAR2) IS
        v_pay NUMBER;
    BEGIN
        SELECT SEQ_PAYMENT.NEXTVAL INTO v_pay FROM DUAL;
        INSERT INTO PAYMENT (PAYMENT_ID, PAYMENT_AMOUNT, PAYMENT_METHOD, PAYMENT_STATUS, PAID_AT, TRANSACTION_ID, BOOKING_ID)
        VALUES (v_pay, p_amount, 'CARD', p_status, SYSDATE, p_tx, p_booking);
        UPDATE BOOKING SET PAYMENT_ID = v_pay WHERE BOOKING_ID = p_booking;
    END;

BEGIN
    v_user1    := f_member('seed_user1');
    v_user2    := f_member('seed_user2');
    v_host2    := f_member('seed_host2');
    v_festival := f_perf('2026 캐치위시 페스티벌');
    v_winter   := f_perf('윈터 라이브 투어 - 서울');
    v_classic  := f_perf('클래식 갈라 콘서트');
    v_jazz     := f_perf('재즈 인 더 시티');

    --wishlist (SECTOR_ID는 NULL. 섹터별 위시는 SECTOR 데이터가 생긴 뒤 추가)
    add_wish(v_user1, v_festival, 1);
    add_wish(v_user1, v_winter,   2);
    add_wish(v_user1, v_jazz,     3);
    add_wish(v_user2, v_festival, 1);
    add_wish(v_user2, v_winter,   2);

    --queue
    -- 페스티벌: 입장(인증 완료) / 만료 / 입장(인증번호 발급, 확인 전)
    add_queue(v_user1, v_festival, 1, 'A', '715204', 'Y');
    add_queue(v_user2, v_festival, 2, 'E', NULL,     NULL);
    add_queue(v_host2, v_festival, 3, 'A', '482913', 'N');
    -- 윈터: 대기 2명 (순번·전체 대기자 수 조회용)
    add_queue(v_host2, v_winter,   1, 'W', NULL,     NULL);
    add_queue(v_user2, v_winter,   2, 'W', NULL,     NULL);

    --booking (+ payment)
    -- 선점(결제 대기) 5분 경과
    v_bid := add_booking(v_user1, v_festival, 'W', NULL, SYSDATE - 5/1440);
    -- 확정
    v_bid := add_booking(v_user1, v_winter,   'C', NULL, SYSDATE - 2);
    add_payment(v_bid, 110000, 'C', 'TXN_SEED_001');
    -- 취소 (환불 계좌 있음)
    v_bid := add_booking(v_user2, v_winter,   'X', '국민 111-222-333444', SYSDATE - 3);
    add_payment(v_bid, 110000, 'C', 'TXN_SEED_002');
    -- 확정
    v_bid := add_booking(v_user2, v_festival, 'C', NULL, SYSDATE - 1);
    add_payment(v_bid, 143000, 'C', 'TXN_SEED_003');
    -- 확정 (취소기한이 지난 공연)
    v_bid := add_booking(v_user1, v_classic,  'C', NULL, SYSDATE - 10);
    add_payment(v_bid, 90000, 'C', 'TXN_SEED_004');
    -- 선점(결제 대기) 30분 경과 = 홀드 만료 대상
    v_bid := add_booking(v_user2, v_jazz,     'W', NULL, SYSDATE - 30/1440);

    COMMIT;
END;
/

-- ---------------------------------------------------------------------
-- 2-3. 재원: 공연장 / 템플릿 / 공연 배치 / 구역·좌석 / 선점 / 결제 / 상태 전이
-- 선행 조건: 현지 seed(공연·등급), 다은과 같은 회원 seed_user1
-- ---------------------------------------------------------------------
-- 0. 공연장 / 템플릿 / 공연 배치 등록
-- 공연장 등록 (현지 seed 공연의 VENUE_NAME 기준, 이미 있으면 건너뜀)
INSERT INTO VENUE (
    VENUE_ID, VENUE_NAME, VENUE_ADDRESS, VENUE_CAPACITY
)
SELECT SEQ_VENUE.NEXTVAL, T.VENUE_NAME, T.VENUE_ADDRESS, 100
FROM (
    SELECT DISTINCT P.VENUE_NAME, P.VENUE_ADDRESS
    FROM PERFORMANCE P
    WHERE P.PERFORMANCE_NAME IN ('2026 캐치위시 페스티벌', '윈터 라이브 투어 - 서울',
                                 '별빛 아래 어쿠스틱 나이트', '뮤지컬 별빛 정원')
      AND NOT EXISTS (SELECT 1 FROM VENUE V WHERE V.VENUE_NAME = P.VENUE_NAME)
) T;

-- 좌석 배치도 템플릿 등록 (IS_TEMPLATE: 'Y', 공연장당 1개)
-- 짧은 접두어를 사용해 공연장 전체 이름을 유지하며 BYTE 단위 절단을 피함.
INSERT INTO SEAT_LAYOUT (
    SEAT_LAYOUT_ID, SEAT_LAYOUT_NAME, IS_TEMPLATE, VENUE_ID, SOURCE_LAYOUT_ID
)
SELECT SEQ_SEAT_LAYOUT.NEXTVAL, T.LAYOUT_NAME, 'Y', T.VENUE_ID, NULL
FROM (
    SELECT DISTINCT
           ('DAO TPL - ' || V.VENUE_NAME) AS LAYOUT_NAME,
           V.VENUE_ID
    FROM PERFORMANCE P
    JOIN VENUE V ON V.VENUE_NAME = P.VENUE_NAME
    WHERE P.PERFORMANCE_NAME IN ('2026 캐치위시 페스티벌', '윈터 라이브 투어 - 서울',
                                 '별빛 아래 어쿠스틱 나이트', '뮤지컬 별빛 정원')
      AND NOT EXISTS (
          SELECT 1 FROM SEAT_LAYOUT X
          WHERE X.SEAT_LAYOUT_NAME = ('DAO TPL - ' || V.VENUE_NAME)
      )
) T;

-- 공연별 좌석 배치도 등록 (IS_TEMPLATE: 'N', 템플릿에서 복사)
-- 짧은 접두어로 공연 이름 전체를 유지함.
INSERT INTO SEAT_LAYOUT (
    SEAT_LAYOUT_ID, SEAT_LAYOUT_NAME, IS_TEMPLATE, VENUE_ID, SOURCE_LAYOUT_ID
)
SELECT SEQ_SEAT_LAYOUT.NEXTVAL,
       ('DAO - ' || P.PERFORMANCE_NAME),
       'N',
       V.VENUE_ID,
       TPL.SEAT_LAYOUT_ID
FROM PERFORMANCE P
JOIN VENUE V ON V.VENUE_NAME = P.VENUE_NAME
JOIN SEAT_LAYOUT TPL ON TPL.VENUE_ID = V.VENUE_ID
                    AND TPL.IS_TEMPLATE = 'Y'
                    AND TPL.SEAT_LAYOUT_NAME = ('DAO TPL - ' || V.VENUE_NAME)
WHERE P.PERFORMANCE_NAME IN ('2026 캐치위시 페스티벌', '윈터 라이브 투어 - 서울',
                             '별빛 아래 어쿠스틱 나이트', '뮤지컬 별빛 정원')
  AND NOT EXISTS (
      SELECT 1 FROM SEAT_LAYOUT X
      WHERE X.SEAT_LAYOUT_NAME = ('DAO - ' || P.PERFORMANCE_NAME)
  );

-- 1. 구역 추가 & 좌석 배치 등록 (M19, M20)
-- 구역 등록: 현지 SEAT_GRADE 1개당 1구역 (가격 높은 순 A, B, C ...)
INSERT INTO SECTOR (
    SECTOR_ID, SECTOR_NAME, GRADE_ID, PERFORMANCE_ID, SEAT_LAYOUT_ID
)
SELECT SEQ_SECTOR.NEXTVAL,
       CHR(64 + T.RN) || '구역(' || T.GRADE_NAME || ')',
       T.GRADE_ID,
       T.PERFORMANCE_ID,
       T.SEAT_LAYOUT_ID
FROM (
    SELECT G.GRADE_ID, G.GRADE_NAME, G.PERFORMANCE_ID, SL.SEAT_LAYOUT_ID,
           ROW_NUMBER() OVER (PARTITION BY G.PERFORMANCE_ID ORDER BY G.PRICE DESC, G.GRADE_ID) AS RN
    FROM SEAT_GRADE G
    JOIN PERFORMANCE P ON P.PERFORMANCE_ID = G.PERFORMANCE_ID
    JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_NAME = ('DAO - ' || P.PERFORMANCE_NAME)
    WHERE P.PERFORMANCE_NAME IN ('2026 캐치위시 페스티벌', '윈터 라이브 투어 - 서울',
                                 '별빛 아래 어쿠스틱 나이트', '뮤지컬 별빛 정원')
      AND NOT EXISTS (
          SELECT 1 FROM SECTOR X
          WHERE X.SEAT_LAYOUT_ID = SL.SEAT_LAYOUT_ID
            AND X.GRADE_ID  = G.GRADE_ID
      )
) T;

-- 좌석 등록: 구역당 2행 x 5열 = 10석, 1행 1~2열은 'B'(잔여석 시연용), 나머지는 'A'
-- ROW_NUM은 DDL 기준 VARCHAR2(10)이라 TO_CHAR 사용 (실제 컬럼이 NUMBER면 T.R로 변경)
INSERT INTO SEAT (
    SEAT_ID, SEAT_LAYOUT_ID, ROW_NUM, COLUMN_NUM, SEAT_NUMBER, SEAT_STATUS, SECTOR_ID
)
SELECT SEQ_SEAT.NEXTVAL,
       T.SEAT_LAYOUT_ID,
       TO_CHAR(T.R),
       T.C,
       T.LETTER || '-' || T.R || '-' || T.C,
       CASE WHEN T.R = 1 AND T.C <= 2 THEN 'B' ELSE 'A' END,
       T.SECTOR_ID
FROM (
    SELECT SC.SECTOR_ID, SC.SEAT_LAYOUT_ID, SUBSTR(SC.SECTOR_NAME, 1, 1) AS LETTER,
           R.LVL AS R, C.LVL AS C
    FROM SECTOR SC
    CROSS JOIN (SELECT LEVEL AS LVL FROM DUAL CONNECT BY LEVEL <= 2) R
    CROSS JOIN (SELECT LEVEL AS LVL FROM DUAL CONNECT BY LEVEL <= 5) C
    WHERE SC.SEAT_LAYOUT_ID IN (SELECT SEAT_LAYOUT_ID FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME LIKE 'DAO - %')
      AND NOT EXISTS (SELECT 1 FROM SEAT X WHERE X.SECTOR_ID = SC.SECTOR_ID)
    ORDER BY SC.SECTOR_ID, R.LVL, C.LVL
) T;

-- 2. 공연 좌석 구조 수정 (M21)
UPDATE SEAT
SET SEAT_NUMBER = 'A-01-01'
WHERE SEAT_LAYOUT_ID = (SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌')
  AND SEAT_NUMBER = 'A-1-1';

-- 3. 좌석 선점 (M22: BOOKING 'W' + BOOKING_SEAT 등록 + SEAT 'A' -> 'H')
--    시나리오 1) A-1-3 결제 성공 / 2) A-1-4 결제 실패 / 3) A-1-5 홀드 만료(11분 전 생성)
--    BOOKING.PAYMENT_ID는 NULL로 넣고, 4번에서 PAYMENT를 만든 뒤 UPDATE로 연결 (FK를 끄지 않음)
-- 시나리오 1: A-1-3
INSERT INTO BOOKING (
    BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID
)
VALUES (
    SEQ_BOOKING.NEXTVAL, 'W', SYSDATE,
    (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = 'seed_user1'),
    NULL,
    (SELECT PERFORMANCE_ID FROM PERFORMANCE WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌')
);

INSERT INTO BOOKING_SEAT (BOOKING_SEAT_ID, PRICE_AT_BOOKING, BOOKING_ID, SEAT_ID)
SELECT SEQ_BOOKING_SEAT.NEXTVAL, G.PRICE,
       (SELECT MAX(B.BOOKING_ID) FROM BOOKING B
        WHERE B.MEMBER_ID = (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = 'seed_user1')
          AND B.PERFORMANCE_ID = (SELECT PERFORMANCE_ID FROM PERFORMANCE WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌')),
       ST.SEAT_ID
FROM SEAT ST
JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_ID = ST.SEAT_LAYOUT_ID
JOIN SECTOR SC      ON SC.SECTOR_ID = ST.SECTOR_ID
JOIN SEAT_GRADE G   ON G.GRADE_ID = SC.GRADE_ID AND G.PERFORMANCE_ID = SC.PERFORMANCE_ID
WHERE SL.SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌'
  AND ST.SEAT_NUMBER = 'A-1-3';

UPDATE SEAT
SET SEAT_STATUS = 'H'
WHERE SEAT_LAYOUT_ID = (SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌')
  AND SEAT_NUMBER = 'A-1-3'
  AND SEAT_STATUS = 'A';

-- 시나리오 2: A-1-4
INSERT INTO BOOKING (
    BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID
)
VALUES (
    SEQ_BOOKING.NEXTVAL, 'W', SYSDATE,
    (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = 'seed_user1'),
    NULL,
    (SELECT PERFORMANCE_ID FROM PERFORMANCE WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌')
);

INSERT INTO BOOKING_SEAT (BOOKING_SEAT_ID, PRICE_AT_BOOKING, BOOKING_ID, SEAT_ID)
SELECT SEQ_BOOKING_SEAT.NEXTVAL, G.PRICE,
       (SELECT MAX(B.BOOKING_ID) FROM BOOKING B
        WHERE B.MEMBER_ID = (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = 'seed_user1')
          AND B.PERFORMANCE_ID = (SELECT PERFORMANCE_ID FROM PERFORMANCE WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌')),
       ST.SEAT_ID
FROM SEAT ST
JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_ID = ST.SEAT_LAYOUT_ID
JOIN SECTOR SC      ON SC.SECTOR_ID = ST.SECTOR_ID
JOIN SEAT_GRADE G   ON G.GRADE_ID = SC.GRADE_ID AND G.PERFORMANCE_ID = SC.PERFORMANCE_ID
WHERE SL.SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌'
  AND ST.SEAT_NUMBER = 'A-1-4';

UPDATE SEAT
SET SEAT_STATUS = 'H'
WHERE SEAT_LAYOUT_ID = (SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌')
  AND SEAT_NUMBER = 'A-1-4'
  AND SEAT_STATUS = 'A';

-- 시나리오 3: A-1-5
INSERT INTO BOOKING (
    BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID
)
VALUES (
    SEQ_BOOKING.NEXTVAL, 'W', SYSDATE - (11 / (24 * 60)),
    (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = 'seed_user1'),
    NULL,
    (SELECT PERFORMANCE_ID FROM PERFORMANCE WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌')
);

INSERT INTO BOOKING_SEAT (BOOKING_SEAT_ID, PRICE_AT_BOOKING, BOOKING_ID, SEAT_ID)
SELECT SEQ_BOOKING_SEAT.NEXTVAL, G.PRICE,
       (SELECT MAX(B.BOOKING_ID) FROM BOOKING B
        WHERE B.MEMBER_ID = (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = 'seed_user1')
          AND B.PERFORMANCE_ID = (SELECT PERFORMANCE_ID FROM PERFORMANCE WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌')),
       ST.SEAT_ID
FROM SEAT ST
JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_ID = ST.SEAT_LAYOUT_ID
JOIN SECTOR SC      ON SC.SECTOR_ID = ST.SECTOR_ID
JOIN SEAT_GRADE G   ON G.GRADE_ID = SC.GRADE_ID AND G.PERFORMANCE_ID = SC.PERFORMANCE_ID
WHERE SL.SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌'
  AND ST.SEAT_NUMBER = 'A-1-5';

UPDATE SEAT
SET SEAT_STATUS = 'H'
WHERE SEAT_LAYOUT_ID = (SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌')
  AND SEAT_NUMBER = 'A-1-5'
  AND SEAT_STATUS = 'A';

-- 4. 결제 처리 요청 / 성공 / 실패 (M23)
-- 결제 요청 등록 (PAYMENT_STATUS = 'P': 결제대기), PAYMENT_ID는 SEQ_PAYMENT로 새로 채번
INSERT INTO PAYMENT (
    PAYMENT_ID, PAYMENT_AMOUNT, PAYMENT_METHOD, PAYMENT_STATUS, PAID_AT, TRANSACTION_ID, BOOKING_ID
)
SELECT SEQ_PAYMENT.NEXTVAL, BS.PRICE_AT_BOOKING, 'CARD', 'P', NULL, 'DAOTEST-001', B.BOOKING_ID
FROM BOOKING B
JOIN BOOKING_SEAT BS ON BS.BOOKING_ID = B.BOOKING_ID
WHERE BS.SEAT_ID = (SELECT ST.SEAT_ID FROM SEAT ST JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_ID = ST.SEAT_LAYOUT_ID
                    WHERE SL.SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌' AND ST.SEAT_NUMBER = 'A-1-3');

INSERT INTO PAYMENT (
    PAYMENT_ID, PAYMENT_AMOUNT, PAYMENT_METHOD, PAYMENT_STATUS, PAID_AT, TRANSACTION_ID, BOOKING_ID
)
SELECT SEQ_PAYMENT.NEXTVAL, BS.PRICE_AT_BOOKING, 'CARD', 'P', NULL, 'DAOTEST-002', B.BOOKING_ID
FROM BOOKING B
JOIN BOOKING_SEAT BS ON BS.BOOKING_ID = B.BOOKING_ID
WHERE BS.SEAT_ID = (SELECT ST.SEAT_ID FROM SEAT ST JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_ID = ST.SEAT_LAYOUT_ID
                    WHERE SL.SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌' AND ST.SEAT_NUMBER = 'A-1-4');

INSERT INTO PAYMENT (
    PAYMENT_ID, PAYMENT_AMOUNT, PAYMENT_METHOD, PAYMENT_STATUS, PAID_AT, TRANSACTION_ID, BOOKING_ID
)
SELECT SEQ_PAYMENT.NEXTVAL, BS.PRICE_AT_BOOKING, 'CARD', 'P', NULL, 'DAOTEST-003', B.BOOKING_ID
FROM BOOKING B
JOIN BOOKING_SEAT BS ON BS.BOOKING_ID = B.BOOKING_ID
WHERE BS.SEAT_ID = (SELECT ST.SEAT_ID FROM SEAT ST JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_ID = ST.SEAT_LAYOUT_ID
                    WHERE SL.SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌' AND ST.SEAT_NUMBER = 'A-1-5');

-- 예매에 결제 번호 연결 (BOOKING.PAYMENT_ID <- PAYMENT.PAYMENT_ID)
UPDATE BOOKING B
SET B.PAYMENT_ID = (SELECT P.PAYMENT_ID FROM PAYMENT P WHERE P.BOOKING_ID = B.BOOKING_ID)
WHERE B.BOOKING_ID IN (SELECT P.BOOKING_ID FROM PAYMENT P
                       WHERE P.TRANSACTION_ID IN ('DAOTEST-001', 'DAOTEST-002', 'DAOTEST-003'));

-- [성공] 결제 완료 ('P' -> 'C'), 예매 확정 ('W' -> 'C'), 좌석 예매완료 ('H' -> 'B')
UPDATE PAYMENT
SET PAYMENT_STATUS = 'C',
    PAID_AT = SYSDATE,
    TRANSACTION_ID = 'DAOTEST-001-OK'
WHERE TRANSACTION_ID = 'DAOTEST-001'
  AND PAYMENT_STATUS = 'P';

UPDATE BOOKING
SET BOOKING_STATUS = 'C'
WHERE PAYMENT_ID IN (SELECT PAYMENT_ID FROM PAYMENT WHERE TRANSACTION_ID = 'DAOTEST-001-OK')
  AND BOOKING_STATUS = 'W';

UPDATE SEAT
SET SEAT_STATUS = 'B'
WHERE SEAT_ID IN (
    SELECT BS.SEAT_ID
    FROM BOOKING_SEAT BS
    JOIN PAYMENT P ON P.BOOKING_ID = BS.BOOKING_ID
    WHERE P.TRANSACTION_ID = 'DAOTEST-001-OK'
)
  AND SEAT_STATUS = 'H';

-- [실패] 결제 실패 ('P' -> 'F'), 예매 취소 ('W' -> 'X'), 좌석 복원 ('H' -> 'A')
UPDATE PAYMENT
SET PAYMENT_STATUS = 'F',
    TRANSACTION_ID = 'DAOTEST-002-FAIL'
WHERE TRANSACTION_ID = 'DAOTEST-002'
  AND PAYMENT_STATUS = 'P';

UPDATE BOOKING
SET BOOKING_STATUS = 'X'
WHERE PAYMENT_ID IN (SELECT PAYMENT_ID FROM PAYMENT WHERE TRANSACTION_ID = 'DAOTEST-002-FAIL')
  AND BOOKING_STATUS = 'W';

UPDATE SEAT
SET SEAT_STATUS = 'A'
WHERE SEAT_ID IN (
    SELECT BS.SEAT_ID
    FROM BOOKING_SEAT BS
    JOIN PAYMENT P ON P.BOOKING_ID = BS.BOOKING_ID
    WHERE P.TRANSACTION_ID = 'DAOTEST-002-FAIL'
)
  AND SEAT_STATUS = 'H';

-- 5. 좌석 상태 전이 & 복원 (M18 / M22)
-- 시나리오 1 예매 취소 ('C' -> 'X')
UPDATE BOOKING
SET BOOKING_STATUS = 'X'
WHERE PAYMENT_ID IN (SELECT PAYMENT_ID FROM PAYMENT WHERE TRANSACTION_ID = 'DAOTEST-001-OK')
  AND BOOKING_STATUS = 'C';

-- 예매 취소('X') 시 좌석 'B' -> 'C'(취소대기), 테스트 예매만 대상
UPDATE SEAT
SET SEAT_STATUS = 'C'
WHERE SEAT_ID IN (
    SELECT BS.SEAT_ID
    FROM BOOKING_SEAT BS
    JOIN BOOKING B ON BS.BOOKING_ID = B.BOOKING_ID
    JOIN PAYMENT P ON P.BOOKING_ID = B.BOOKING_ID
    WHERE B.BOOKING_STATUS = 'X'
      AND P.TRANSACTION_ID LIKE 'DAOTEST%'
)
  AND SEAT_STATUS = 'B';

-- 홀드 만료('W'이고 10분 경과) 시 좌석 'H' -> 'A' (테스트 배치만 대상)
UPDATE SEAT
SET SEAT_STATUS = 'A'
WHERE SEAT_ID IN (
    SELECT BS.SEAT_ID
    FROM BOOKING_SEAT BS
    JOIN BOOKING B ON BS.BOOKING_ID = B.BOOKING_ID
    WHERE B.BOOKING_STATUS = 'W'
      AND B.CREATED_AT < SYSDATE - (10 / (24 * 60))
)
  AND SEAT_STATUS = 'H'
  AND SEAT_LAYOUT_ID IN (SELECT SEAT_LAYOUT_ID FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME LIKE 'DAO - %');

-- 홀드 만료 후속: 결제 'F', 예매 'X' (좌석 복원 이후 실행)
UPDATE PAYMENT
SET PAYMENT_STATUS = 'F',
    TRANSACTION_ID = 'DAOTEST-003-EXPIRED'
WHERE TRANSACTION_ID = 'DAOTEST-003'
  AND PAYMENT_STATUS = 'P'
  AND BOOKING_ID IN (SELECT BOOKING_ID FROM BOOKING
                     WHERE BOOKING_STATUS = 'W' AND CREATED_AT < SYSDATE - (10 / (24 * 60)));

UPDATE BOOKING
SET BOOKING_STATUS = 'X'
WHERE PAYMENT_ID IN (SELECT PAYMENT_ID FROM PAYMENT WHERE TRANSACTION_ID = 'DAOTEST-003-EXPIRED')
  AND BOOKING_STATUS = 'W';

-- 취소대기('C') 좌석을 재오픈 시점에 'A'(예매가능)로 복원
UPDATE SEAT
SET SEAT_STATUS = 'A'
WHERE SEAT_LAYOUT_ID = (SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌')
  AND SEAT_STATUS = 'C';

COMMIT;

-- ---------------------------------------------------------------------
-- 2-4. 지우: 취소표 재오픈 일정·알림·선예매 코드·선예매 인증
-- 선행 조건: 현지 seed에 회원(seed_user1, seed_user2)과 공연(2026 캐치위시 페스티벌),
--           다은 seed에 두 회원의 해당 공연 WISHLIST(SECTOR_ID NULL)가 각각 1건씩 있어야 함.
-- 의도: 취소표 재오픈 일정 등록, 두 회원의 위시와 취소표 알림 연결,
--       유효·만료 선예매 코드 검증, 회원1의 선예매 인증 완료 상태 조회
-- 예상: CANCELED_TICKET_SCHEDULE 1건 / NOTIFICATION 2건 /
--       PRESALE_CODE 2건 (유효 1, 만료 1) / CHECK_AUTH_CODE 1건 (인증 완료 Y)
-- ---------------------------------------------------------------------
DECLARE
    v_performance_id PERFORMANCE.PERFORMANCE_ID%TYPE;
    v_user1_id       MEMBER.MEMBER_ID%TYPE;
    v_user2_id       MEMBER.MEMBER_ID%TYPE;
    v_wish1_id       WISHLIST.WISHLIST_ID%TYPE;
    v_wish2_id       WISHLIST.WISHLIST_ID%TYPE;
    v_schedule_id    CANCELED_TICKET_SCHEDULE.CANCELED_TICKET_SCHEDULE_ID%TYPE;

    PROCEDURE add_notification(
        p_member_id MEMBER.MEMBER_ID%TYPE,
        p_wishlist_id WISHLIST.WISHLIST_ID%TYPE
    ) IS
    BEGIN
        INSERT INTO NOTIFICATION (
            NOTIFICATION_ID, NOTIFICATION_TYPE, SEND_STATUS,
            CREATED_AT, MEMBER_ID, IS_READ, WISHLIST_ID,
            CANCELED_TICKET_SCHEDULE_ID
        )
        VALUES (
            SEQ_NOTIFICATION.NEXTVAL, 'CANCEL', 'N',
            SYSDATE, p_member_id, 'N', p_wishlist_id,
            v_schedule_id
        );
    END;

BEGIN
    SELECT PERFORMANCE_ID INTO v_performance_id
    FROM PERFORMANCE
    WHERE PERFORMANCE_NAME = '2026 캐치위시 페스티벌';

    SELECT MEMBER_ID INTO v_user1_id
    FROM MEMBER
    WHERE LOGIN_ID = 'seed_user1';

    SELECT MEMBER_ID INTO v_user2_id
    FROM MEMBER
    WHERE LOGIN_ID = 'seed_user2';

    -- 다은 seed에서 등록한 공연 단위 위시 조회
    SELECT WISHLIST_ID INTO v_wish1_id
    FROM WISHLIST
    WHERE MEMBER_ID = v_user1_id
      AND PERFORMANCE_ID = v_performance_id
      AND SECTOR_ID IS NULL;

    SELECT WISHLIST_ID INTO v_wish2_id
    FROM WISHLIST
    WHERE MEMBER_ID = v_user2_id
      AND PERFORMANCE_ID = v_performance_id
      AND SECTOR_ID IS NULL;

    -- 취소표 재오픈 일정 1건
    -- CREATED_AT 고정값은 cleanup에서 이 seed 행을 식별하는 데 사용
    INSERT INTO CANCELED_TICKET_SCHEDULE (
        CANCELED_TICKET_SCHEDULE_ID, OPEN_AT,
        SCHEDULE_STATUS, CREATED_AT, PERFORMANCE_ID
    )
    VALUES (
        SEQ_CANCELED_TICKET_SCHEDULE.NEXTVAL, SYSDATE + 1,
        'P', DATE '2000-01-01', v_performance_id
    )
    RETURNING CANCELED_TICKET_SCHEDULE_ID INTO v_schedule_id;

    -- 두 회원의 위시에 같은 취소표 일정 연결
    add_notification(v_user1_id, v_wish1_id);
    add_notification(v_user2_id, v_wish2_id);

    -- 유효한 선예매 코드
    INSERT INTO PRESALE_CODE (
        CODE_ID, PERFORMANCE_ID, CODE_VALUE,
        SEAT_LIMIT, START_AT, END_AT, CREATED_AT
    )
    VALUES (
        SEQ_PRESALE_CODE.NEXTVAL, v_performance_id, 'TESTCODE001',
        2, SYSDATE - 1, SYSDATE + 7, SYSDATE
    );

    -- 만료된 선예매 코드
    INSERT INTO PRESALE_CODE (
        CODE_ID, PERFORMANCE_ID, CODE_VALUE,
        SEAT_LIMIT, START_AT, END_AT, CREATED_AT
    )
    VALUES (
        SEQ_PRESALE_CODE.NEXTVAL, v_performance_id, 'TESTCODE002',
        2, SYSDATE - 7, SYSDATE - 1, SYSDATE
    );

    -- 회원1 선예매 인증 완료
    -- CREATED_AT 고정값은 cleanup에서 이 seed 행을 식별하는 데 사용
    INSERT INTO CHECK_AUTH_CODE (
        AUTH_CODE_ID, MEMBER_ID, PERFORMANCE_ID,
        IS_VERIFIED, EXPIRES_AT, CREATED_AT
    )
    VALUES (
        SEQ_AUTH_CODE.NEXTVAL, v_user1_id, v_performance_id,
        'Y', SYSDATE + 7, DATE '2000-01-01'
    );

    COMMIT;
END;
/


-- #####################################################################
-- PART 3. CHECK  (현지 -> 다은 -> 재원 -> 지우)
-- #####################################################################

-- ---------------------------------------------------------------------
-- 3-1. 현지 check (해시 / 상태·장르별 개수 / 등급·증빙 수 / 대관 기간 포함 여부)
-- ---------------------------------------------------------------------
SELECT LOGIN_ID, MEMBER_ROLE,
       CASE WHEN PASSWORD_HASH = LOWER(RAWTOHEX(STANDARD_HASH(
                 CASE WHEN MEMBER_ROLE = 'ADMIN' THEN 'admin1234' ELSE 'test1234' END, 'SHA256')))
            THEN 'Y' ELSE 'N' END AS HASH_OK
FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\' ORDER BY LOGIN_ID;

SELECT APPROVAL_STATUS, GENRE, COUNT(*) AS CNT
FROM PERFORMANCE
WHERE REGISTRANT_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
GROUP BY APPROVAL_STATUS, GENRE ORDER BY APPROVAL_STATUS, GENRE;

SELECT p.PERFORMANCE_NAME,
       (SELECT COUNT(*) FROM SEAT_GRADE g WHERE g.PERFORMANCE_ID = p.PERFORMANCE_ID) AS GRADES,
       (SELECT COUNT(*) FROM VENUE_RENTAL_PROOF r WHERE r.PERFORMANCE_ID = p.PERFORMANCE_ID) AS PROOFS
FROM PERFORMANCE p
WHERE p.REGISTRANT_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
ORDER BY p.PERFORMANCE_ID;

SELECT p.PERFORMANCE_NAME,
       CASE WHEN p.SHOW_AT BETWEEN r.RENTAL_START_AT AND r.RENTAL_END_AT THEN 'Y' ELSE 'N' END AS COVERS
FROM PERFORMANCE p JOIN VENUE_RENTAL_PROOF r ON r.PERFORMANCE_ID = p.PERFORMANCE_ID
WHERE p.REGISTRANT_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
ORDER BY COVERS, p.PERFORMANCE_NAME;

-- ---------------------------------------------------------------------
-- 3-2. 다은 check (건수 / 상태별 개수 / 예매-결제 연결 / 회원별 위시 순서)
--      재원 seed 포함 기준 예상: WISHLIST 5 / QUEUE 5 / BOOKING 9 / PAYMENT 7
--      (QUEUE W 2, A 2, E 1 / BOOKING W 2, C 3, X 4)
-- ---------------------------------------------------------------------
SELECT 'WISHLIST' AS TBL, COUNT(*) AS CNT FROM WISHLIST
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
UNION ALL
SELECT 'QUEUE', COUNT(*) FROM QUEUE
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
UNION ALL
SELECT 'BOOKING', COUNT(*) FROM BOOKING
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
UNION ALL
SELECT 'PAYMENT', COUNT(*) FROM PAYMENT
WHERE BOOKING_ID IN (SELECT BOOKING_ID FROM BOOKING
                     WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\'));

SELECT 'QUEUE' AS TBL, STATUS AS ST, COUNT(*) AS CNT FROM QUEUE
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
GROUP BY STATUS
UNION ALL
SELECT 'BOOKING', BOOKING_STATUS, COUNT(*) FROM BOOKING
WHERE MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
GROUP BY BOOKING_STATUS
ORDER BY 1, 2;

SELECT b.BOOKING_ID, b.BOOKING_STATUS, p.PAYMENT_STATUS,
       CASE WHEN b.PAYMENT_ID = p.PAYMENT_ID AND p.BOOKING_ID = b.BOOKING_ID THEN 'Y' ELSE 'N' END AS LINK_OK
FROM BOOKING b LEFT JOIN PAYMENT p ON p.BOOKING_ID = b.BOOKING_ID
WHERE b.MEMBER_ID IN (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID LIKE 'seed\_%' ESCAPE '\')
ORDER BY b.BOOKING_ID;

SELECT m.LOGIN_ID, pf.PERFORMANCE_NAME, w.PRIORITY
FROM WISHLIST w JOIN MEMBER m ON m.MEMBER_ID = w.MEMBER_ID
                JOIN PERFORMANCE pf ON pf.PERFORMANCE_ID = w.PERFORMANCE_ID
WHERE m.LOGIN_ID LIKE 'seed\_%' ESCAPE '\'
ORDER BY m.LOGIN_ID, w.PRIORITY;

-- ---------------------------------------------------------------------
-- 3-3. 재원 check (M19, M20, M22, M23)
-- ---------------------------------------------------------------------
-- 공연장 목록 조회
SELECT VENUE_ID, VENUE_NAME, VENUE_ADDRESS, VENUE_CAPACITY
FROM VENUE
ORDER BY VENUE_NAME;

-- 공연장 상세 조회 (테스트 템플릿 배치와 연결된 공연장)
SELECT DISTINCT
       V.VENUE_ID, V.VENUE_NAME, V.VENUE_ADDRESS, V.VENUE_CAPACITY
FROM VENUE V
JOIN SEAT_LAYOUT SL ON V.VENUE_ID = SL.VENUE_ID
WHERE SL.SEAT_LAYOUT_NAME LIKE 'DAO%'
  AND SL.IS_TEMPLATE = 'Y'
ORDER BY V.VENUE_NAME;

-- 템플릿 -> 공연 배치 연결 확인
SELECT SL.SEAT_LAYOUT_ID, SL.SEAT_LAYOUT_NAME, SL.IS_TEMPLATE, SL.SOURCE_LAYOUT_ID, V.VENUE_NAME
FROM SEAT_LAYOUT SL
JOIN VENUE V ON V.VENUE_ID = SL.VENUE_ID
WHERE SL.SEAT_LAYOUT_NAME LIKE 'DAO%'
ORDER BY V.VENUE_NAME, SL.IS_TEMPLATE DESC, SL.SEAT_LAYOUT_ID;

-- 공연별 구역 / 등급 / 좌석 수 (예상: 구역당 10석, 'B' 2석)
SELECT P.PERFORMANCE_NAME, SC.SECTOR_NAME, G.GRADE_NAME, G.PRICE,
       COUNT(ST.SEAT_ID) AS TOTAL_SEATS,
       SUM(CASE WHEN ST.SEAT_STATUS = 'A' THEN 1 ELSE 0 END) AS AVAILABLE,
       SUM(CASE WHEN ST.SEAT_STATUS = 'B' THEN 1 ELSE 0 END) AS BOOKED
FROM SEAT_LAYOUT SL
JOIN SECTOR SC     ON SC.SEAT_LAYOUT_ID = SL.SEAT_LAYOUT_ID
JOIN PERFORMANCE P ON P.PERFORMANCE_ID  = SC.PERFORMANCE_ID
JOIN SEAT_GRADE G  ON G.GRADE_ID = SC.GRADE_ID AND G.PERFORMANCE_ID = SC.PERFORMANCE_ID
LEFT JOIN SEAT ST  ON ST.SECTOR_ID = SC.SECTOR_ID
WHERE SL.SEAT_LAYOUT_NAME LIKE 'DAO - %'
GROUP BY P.PERFORMANCE_ID, P.PERFORMANCE_NAME, SC.SECTOR_ID, SC.SECTOR_NAME, G.GRADE_NAME, G.PRICE
ORDER BY P.PERFORMANCE_ID, SC.SECTOR_ID;

-- 템플릿 구조 + 좌석 조회 ('2026 캐치위시 페스티벌')
SELECT SL.SEAT_LAYOUT_ID, SL.VENUE_ID, SL.SEAT_LAYOUT_NAME, SL.IS_TEMPLATE,
       SC.SECTOR_ID, SC.SECTOR_NAME,
       ST.SEAT_ID, ST.ROW_NUM, ST.COLUMN_NUM, ST.SEAT_NUMBER, ST.SEAT_STATUS
FROM SEAT_LAYOUT SL
JOIN SECTOR SC ON SL.SEAT_LAYOUT_ID = SC.SEAT_LAYOUT_ID
LEFT JOIN SEAT ST ON SC.SECTOR_ID = ST.SECTOR_ID
WHERE SL.SEAT_LAYOUT_ID = (SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_NAME = 'DAO - 2026 캐치위시 페스티벌')
ORDER BY SC.SECTOR_ID, ST.ROW_NUM, ST.COLUMN_NUM;

-- 예매 / 결제 / 좌석 상태 연동 결과
-- 예상: A-1-3 좌석 A / 예매 X / 결제 C / DAOTEST-001-OK
--       A-1-4 좌석 A / 예매 X / 결제 F / DAOTEST-002-FAIL
--       A-1-5 좌석 A / 예매 X / 결제 F / DAOTEST-003-EXPIRED
SELECT ST.SEAT_NUMBER, ST.SEAT_STATUS, B.BOOKING_STATUS, P.PAYMENT_STATUS, P.TRANSACTION_ID, P.PAYMENT_AMOUNT
FROM BOOKING_SEAT BS
JOIN SEAT ST    ON ST.SEAT_ID = BS.SEAT_ID
JOIN BOOKING B  ON B.BOOKING_ID = BS.BOOKING_ID
JOIN PAYMENT P  ON P.BOOKING_ID = B.BOOKING_ID
WHERE P.TRANSACTION_ID LIKE 'DAOTEST%'
ORDER BY ST.SEAT_NUMBER;

-- 재원 seed 전체 검증 (M19/M20/M22/M23)
-- 1) 배치도별 구역/좌석 합계 및 좌석 상태 분포
SELECT SL.SEAT_LAYOUT_NAME, SL.IS_TEMPLATE, COUNT(DISTINCT SC.SECTOR_ID) AS SECTOR_COUNT,
       COUNT(ST.SEAT_ID) AS SEAT_COUNT,
       SUM(CASE WHEN ST.SEAT_STATUS = 'A' THEN 1 ELSE 0 END) AS STATUS_A,
       SUM(CASE WHEN ST.SEAT_STATUS = 'B' THEN 1 ELSE 0 END) AS STATUS_B,
       SUM(CASE WHEN ST.SEAT_STATUS = 'H' THEN 1 ELSE 0 END) AS STATUS_H,
       SUM(CASE WHEN ST.SEAT_STATUS = 'C' THEN 1 ELSE 0 END) AS STATUS_C
FROM SEAT_LAYOUT SL
LEFT JOIN SECTOR SC ON SC.SEAT_LAYOUT_ID = SL.SEAT_LAYOUT_ID
LEFT JOIN SEAT ST ON ST.SECTOR_ID = SC.SECTOR_ID
WHERE SL.SEAT_LAYOUT_NAME LIKE 'DAO%'
GROUP BY SL.SEAT_LAYOUT_ID, SL.SEAT_LAYOUT_NAME, SL.IS_TEMPLATE
ORDER BY SL.IS_TEMPLATE DESC, SL.SEAT_LAYOUT_NAME;

-- 2) BOOKING_SEAT와 BOOKING, PAYMENT, SEAT 연결 상세 (DAO 거래 포함)
SELECT P.PERFORMANCE_NAME, B.BOOKING_ID, B.BOOKING_STATUS,
       PAY.PAYMENT_ID, PAY.PAYMENT_STATUS, PAY.TRANSACTION_ID,
       ST.SEAT_NUMBER, ST.SEAT_STATUS, BS.PRICE_AT_BOOKING
FROM BOOKING B
JOIN BOOKING_SEAT BS ON BS.BOOKING_ID = B.BOOKING_ID
JOIN SEAT ST ON ST.SEAT_ID = BS.SEAT_ID
LEFT JOIN PAYMENT PAY ON PAY.BOOKING_ID = B.BOOKING_ID
LEFT JOIN PERFORMANCE P ON P.PERFORMANCE_ID = B.PERFORMANCE_ID
WHERE B.MEMBER_ID = (SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = 'seed_user1')
  AND B.PERFORMANCE_ID IN (SELECT PERFORMANCE_ID FROM PERFORMANCE
                           WHERE PERFORMANCE_NAME IN ('2026 캐치위시 페스티벌', '윈터 라이브 투어 - 서울',
                                                     '별빛 아래 어쿠스틱 나이트', '뮤지컬 별빛 정원'))
ORDER BY B.BOOKING_ID, ST.SEAT_NUMBER;

-- 3) DAO 테스트 거래 ID별 결과 확인
SELECT PAY.TRANSACTION_ID, PAY.PAYMENT_STATUS, PAY.PAYMENT_AMOUNT,
       B.BOOKING_STATUS, COUNT(BS.SEAT_ID) AS LINKED_SEATS,
       MIN(ST.SEAT_STATUS) AS SEAT_STATUS
FROM PAYMENT PAY
JOIN BOOKING B ON B.BOOKING_ID = PAY.BOOKING_ID
LEFT JOIN BOOKING_SEAT BS ON BS.BOOKING_ID = B.BOOKING_ID
LEFT JOIN SEAT ST ON ST.SEAT_ID = BS.SEAT_ID
WHERE PAY.TRANSACTION_ID LIKE 'DAOTEST%'
GROUP BY PAY.TRANSACTION_ID, PAY.PAYMENT_STATUS, PAY.PAYMENT_AMOUNT, B.BOOKING_STATUS
ORDER BY PAY.TRANSACTION_ID;

-- 4) 좌석/구역/배치도 FK 및 연결 누락 점검 (결과 0건이 정상)
SELECT 'SECTOR_LAYOUT_MISSING' AS CHECK_TYPE, COUNT(*) AS ERROR_COUNT
FROM SECTOR SC LEFT JOIN SEAT_LAYOUT SL ON SL.SEAT_LAYOUT_ID = SC.SEAT_LAYOUT_ID
WHERE SL.SEAT_LAYOUT_ID IS NULL
UNION ALL
SELECT 'SEAT_SECTOR_MISSING', COUNT(*)
FROM SEAT ST LEFT JOIN SECTOR SC ON SC.SECTOR_ID = ST.SECTOR_ID
WHERE SC.SECTOR_ID IS NULL
UNION ALL
SELECT 'BOOKING_SEAT_SEAT_MISSING', COUNT(*)
FROM BOOKING_SEAT BS LEFT JOIN SEAT ST ON ST.SEAT_ID = BS.SEAT_ID
WHERE ST.SEAT_ID IS NULL;

-- FK 복구 확인 (STATUS = ENABLED, VALIDATED = VALIDATED 이어야 함)
SELECT CONSTRAINT_NAME, STATUS, VALIDATED
FROM USER_CONSTRAINTS
WHERE CONSTRAINT_NAME IN ('BOOKING_PAYMENT_FK', 'PAYMENT_BOOKING_FK');

-- ---------------------------------------------------------------------
-- 3-4. 지우 check (결과 조회)
-- ---------------------------------------------------------------------
SELECT 'SCHEDULE' AS T, COUNT(*) AS CNT FROM CANCELED_TICKET_SCHEDULE WHERE CREATED_AT = DATE '2000-01-01'
UNION ALL SELECT 'NOTIFICATION', COUNT(*) FROM NOTIFICATION
  WHERE CANCELED_TICKET_SCHEDULE_ID IN (SELECT CANCELED_TICKET_SCHEDULE_ID FROM CANCELED_TICKET_SCHEDULE WHERE CREATED_AT = DATE '2000-01-01')
UNION ALL SELECT 'PRESALE', COUNT(*) FROM PRESALE_CODE WHERE CODE_VALUE IN ('TESTCODE001','TESTCODE002')
UNION ALL SELECT 'AUTH', COUNT(*) FROM CHECK_AUTH_CODE WHERE CREATED_AT = DATE '2000-01-01';
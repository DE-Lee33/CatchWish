-- ⚠️ 이미 팀 공용 DB에 반영됨. 재실행 금지
-- 변경 내용: BOOKING 테이블 PAYMENT_ID NULL 허용
-- 변경 일자: 2026-11-11

ALTER TABLE BOOKING
MODIFY PAYMENT_ID NULL;

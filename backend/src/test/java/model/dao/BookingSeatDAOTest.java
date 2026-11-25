package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.domain.enums.BookingStatus;
import model.domain.enums.SeatStatus;

/**
 * BookingSeatDAO 테스트
 * - 테스트용 VENUE/SEAT_LAYOUT/SECTOR/SEAT/BOOKING을 직접 만들고 끝나면 지운다.
 * - ID는 NEXTVAL을 먼저 받아서 사용한다 (MAX 조회는 공용 DB에서 남의 행을 가리킬 수 있음).
 * - 전역 UPDATE(복원)는 참여용 오버로드로 실행하고 rollback해서 공용 DB에 반영하지 않는다.
 */
public class BookingSeatDAOTest {

    private BookingSeatDAO dao;

    private long memberId, gradeId, performanceId;
    private long venueId, layoutId, sectorId, seatId;
    private final List<Long> bookingIds = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        dao = new BookingSeatDAO();
        System.out.println("\n[SETUP] BookingSeatDAO 생성 완료, db.properties 확인됨");

        memberId = queryLong("SELECT MIN(MEMBER_ID) FROM MEMBER");
        assumeTrue("MEMBER 데이터가 필요합니다.", memberId > 0);
        System.out.println("[SETUP] 기존 MEMBER_ID 사용: " + memberId);

        // GRADE_ID와 PERFORMANCE_ID는 같은 행에서 가져와야 FK가 맞는다
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT GRADE_ID, PERFORMANCE_ID FROM SEAT_GRADE WHERE ROWNUM = 1", null);
            ResultSet rs = jdbc.executeQuery();
            assumeTrue("SEAT_GRADE 데이터가 필요합니다.", rs.next());
            gradeId = rs.getLong(1);
            performanceId = rs.getLong(2);
        } finally {
            jdbc.close();
        }
        System.out.println("[SETUP] 기존 GRADE_ID=" + gradeId + ", PERFORMANCE_ID=" + performanceId + " 사용");

        // 테스트 데이터는 하나의 트랜잭션으로 만든다
        jdbc = new JDBCUtil();
        try {
            venueId = nextVal(jdbc, "SEQ_VENUE");
            execute(jdbc, "INSERT INTO VENUE (VENUE_ID, VENUE_NAME, VENUE_CAPACITY) VALUES (?, ?, ?)",
                    venueId, "테스트공연장_BS", 100);
            System.out.println("[SETUP] 테스트 VENUE 생성: " + venueId);

            layoutId = nextVal(jdbc, "SEQ_SEAT_LAYOUT");
            execute(jdbc, "INSERT INTO SEAT_LAYOUT (SEAT_LAYOUT_ID, VENUE_ID, SEAT_LAYOUT_NAME, IS_TEMPLATE) "
                    + "VALUES (?, ?, ?, 'Y')", layoutId, venueId, "테스트레이아웃_BS");
            System.out.println("[SETUP] 테스트 SEAT_LAYOUT 생성: " + layoutId);

            sectorId = nextVal(jdbc, "SEQ_SECTOR");
            execute(jdbc, "INSERT INTO SECTOR (SECTOR_ID, SEAT_LAYOUT_ID, SECTOR_NAME, GRADE_ID, PERFORMANCE_ID) "
                    + "VALUES (?, ?, ?, ?, ?)", sectorId, layoutId, "테스트구역_BS", gradeId, performanceId);
            System.out.println("[SETUP] 테스트 SECTOR 생성: " + sectorId);

            seatId = nextVal(jdbc, "SEQ_SEAT");
            execute(jdbc, "INSERT INTO SEAT (SEAT_ID, SEAT_LAYOUT_ID, SECTOR_ID, ROW_NUM, COLUMN_NUM, SEAT_NUMBER, SEAT_STATUS) "
                    + "VALUES (?, ?, ?, 'A', 1, 'TEST-1', ?)",
                    seatId, layoutId, sectorId, SeatStatus.AVAILABLE.getCode());
            System.out.println("[SETUP] 테스트 SEAT 생성 (상태 A): " + seatId);

            jdbc.commit();
        } catch (Exception e) {
            jdbc.rollback();
            throw e;
        } finally {
            jdbc.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        // 외래키 역순. 한 단계가 실패해도 나머지는 계속 정리한다
        for (long id : bookingIds) {
            deleteQuietly("BOOKING_SEAT(BOOKING_ID)", "DELETE FROM BOOKING_SEAT WHERE BOOKING_ID = ?", id);
        }
        deleteQuietly("BOOKING_SEAT(SEAT_ID)", "DELETE FROM BOOKING_SEAT WHERE SEAT_ID = ?", seatId);
        for (long id : bookingIds) {
            deleteQuietly("BOOKING", "DELETE FROM BOOKING WHERE BOOKING_ID = ?", id);
        }
        deleteQuietly("SEAT", "DELETE FROM SEAT WHERE SEAT_ID = ?", seatId);
        deleteQuietly("SECTOR", "DELETE FROM SECTOR WHERE SECTOR_ID = ?", sectorId);
        deleteQuietly("SEAT_LAYOUT", "DELETE FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_ID = ?", layoutId);
        deleteQuietly("VENUE", "DELETE FROM VENUE WHERE VENUE_ID = ?", venueId);
        System.out.println("[TEARDOWN] 정리 완료");
    }

    // ===================== updateSeatToHeld =====================

    @Test
    public void updateSeatToHeld_availableSeatBecomesHeld() throws Exception {
        System.out.println("\n[TEST] updateSeatToHeld (A → H) 시작");
        int result = dao.updateSeatToHeld(seatId);
        System.out.println("[INFO] 호출 결과: " + result);
        assertEquals(1, result);
        assertEquals(SeatStatus.HELD.getCode(), seatStatus(seatId));
        System.out.println("[PASS] A → H 변경 성공, DB 재조회로 확인");
    }

    @Test
    public void updateSeatToHeld_cannotHoldTwice() throws Exception {
        System.out.println("\n[TEST] updateSeatToHeld 중복 선점 시작");
        int first = dao.updateSeatToHeld(seatId);
        int second = dao.updateSeatToHeld(seatId);
        System.out.println("[INFO] 첫 번째: " + first + ", 두 번째: " + second);
        assertEquals(1, first);
        assertEquals("이미 선점된 좌석은 중복 선점 불가", 0, second);
        assertEquals(SeatStatus.HELD.getCode(), seatStatus(seatId));
        System.out.println("[PASS] 이미 선점된 좌석은 0건 변경 (중복 선점 방지)");
    }

    @Test
    public void updateSeatToHeld_nonExistingSeat() throws Exception {
        System.out.println("\n[TEST] updateSeatToHeld 없는 좌석 시작");
        int result = dao.updateSeatToHeld(Long.MAX_VALUE);
        System.out.println("[INFO] 호출 결과: " + result);
        assertEquals(0, result);
        System.out.println("[PASS] 없는 좌석은 0건 변경");
    }

    // ===================== insertBookingSeat =====================

    @Test
    public void insertBookingSeat_savesRowWithPrice() throws Exception {
        System.out.println("\n[TEST] insertBookingSeat 시작");
        long bookingId = createBooking(BookingStatus.WAITING_PAYMENT.getCode());

        int result = dao.insertBookingSeat(bookingId, seatId, 50000.0);
        System.out.println("[INFO] insert 결과: " + result);
        assertEquals(1, result);

        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters(
                    "SELECT PRICE_AT_BOOKING FROM BOOKING_SEAT WHERE BOOKING_ID = ? AND SEAT_ID = ?",
                    new Object[] { bookingId, seatId });
            ResultSet rs = jdbc.executeQuery();
            assertTrue("BOOKING_SEAT 행이 저장되어야 합니다.", rs.next());
            double price = rs.getDouble(1);
            System.out.println("[INFO] 저장된 가격: " + price);
            assertEquals(50000.0, price, 0.001);
            assertFalse("정확히 1행이어야 합니다.", rs.next());
        } finally {
            jdbc.close();
        }
        System.out.println("[PASS] BOOKING_SEAT 등록 성공 (bookingId=" + bookingId
                + ", seatId=" + seatId + ", price=50000.0)");
    }

    // ===================== releaseSeatsByBookingId =====================

    @Test
    public void releaseSeatsByBookingId_heldSeatBecomesAvailable() throws Exception {
        System.out.println("\n[TEST] releaseSeatsByBookingId (H → A) 시작");
        long bookingId = createBooking(BookingStatus.WAITING_PAYMENT.getCode());
        int held = dao.updateSeatToHeld(seatId);
        dao.insertBookingSeat(bookingId, seatId, 50000.0);
        System.out.println("[INFO] 선행: 좌석 홀드 결과 " + held + ", BOOKING_SEAT 등록 완료");

        int result = dao.releaseSeatsByBookingId(bookingId);
        System.out.println("[INFO] 홀드 해제된 좌석 수: " + result);
        assertEquals(1, result);
        assertEquals(SeatStatus.AVAILABLE.getCode(), seatStatus(seatId));
        System.out.println("[PASS] H → A 복원 성공");
    }

    @Test
    public void releaseSeatsByBookingId_bookedSeatIsNotReleased() throws Exception {
        System.out.println("\n[TEST] releaseSeatsByBookingId (B 좌석은 유지) 시작");
        long bookingId = createBooking(BookingStatus.WAITING_PAYMENT.getCode());
        dao.insertBookingSeat(bookingId, seatId, 50000.0);
        setSeatStatus(seatId, SeatStatus.BOOKED.getCode());
        System.out.println("[INFO] 선행: 좌석 상태를 B로 설정");

        int result = dao.releaseSeatsByBookingId(bookingId);
        System.out.println("[INFO] 해제된 좌석 수: " + result);
        assertEquals("H가 아닌 좌석은 건드리지 않아야 합니다.", 0, result);
        assertEquals(SeatStatus.BOOKED.getCode(), seatStatus(seatId));
        System.out.println("[PASS] B 좌석은 해제되지 않음");
    }

    // ===================== 트랜잭션 참여용 =====================

    @Test
    public void participatingMethod_rollbackLeavesNoChange() throws Exception {
        System.out.println("\n[TEST] 참여용 메소드 rollback 시작");
        JDBCUtil jdbc = new JDBCUtil();
        try {
            int result = dao.updateSeatToHeld(jdbc, seatId);
            System.out.println("[INFO] 참여용 호출 결과: " + result + " (commit 전)");
            assertEquals(1, result);
            jdbc.rollback();
            System.out.println("[INFO] rollback 실행");
        } finally {
            jdbc.close();
        }
        assertEquals("롤백했으므로 좌석은 그대로 A", SeatStatus.AVAILABLE.getCode(), seatStatus(seatId));
        System.out.println("[PASS] rollback 후 좌석 상태 A 유지");
    }

    // ===================== restoreCancelledAndExpiredSeats =====================
    // 공용 DB 전체가 대상인 UPDATE라서 참여용 오버로드로 실행하고 rollback한다.

    @Test
    public void restore_heldSeatOfCancelledBookingBecomesAvailable() throws Exception {
        System.out.println("\n[TEST] restoreCancelledAndExpiredSeats (취소 예매의 H 좌석) 시작");
        long cancelled = createBooking(BookingStatus.CANCELED.getCode());
        dao.insertBookingSeat(cancelled, seatId, 50000.0);
        setSeatStatus(seatId, SeatStatus.HELD.getCode());
        System.out.println("[INFO] 선행: 취소 예매(" + cancelled + ")에 좌석 연결, 좌석 상태 H");

        JDBCUtil jdbc = new JDBCUtil();
        try {
            int restored = dao.restoreCancelledAndExpiredSeats(jdbc);
            System.out.println("[INFO] 복원된 좌석 수: " + restored + " (rollback 예정, 공용 DB 미반영)");
            assertTrue(restored >= 1);
            assertEquals(SeatStatus.AVAILABLE.getCode(), seatStatus(jdbc, seatId));   // 같은 트랜잭션에서 확인
            System.out.println("[PASS] 트랜잭션 안에서 H → A 확인");
            jdbc.rollback();
        } finally {
            jdbc.close();
        }
        assertEquals("롤백 후 원래 상태", SeatStatus.HELD.getCode(), seatStatus(seatId));
        System.out.println("[PASS] rollback 후 원래 상태(H) 유지, 공용 DB 영향 없음");
    }

    @Test
    public void restore_seatWithActiveBookingIsNotRestored() throws Exception {
        System.out.println("\n[TEST] restoreCancelledAndExpiredSeats (활성 예매 좌석 제외) 시작");
        // 같은 좌석이 취소 예매와 최근 결제대기 예매에 모두 연결된 경우 (이중 예매 방지)
        long cancelled = createBooking(BookingStatus.CANCELED.getCode());
        long active = createBooking(BookingStatus.WAITING_PAYMENT.getCode());
        dao.insertBookingSeat(cancelled, seatId, 50000.0);
        dao.insertBookingSeat(active, seatId, 50000.0);
        setSeatStatus(seatId, SeatStatus.HELD.getCode());
        System.out.println("[INFO] 선행: 취소 예매(" + cancelled + ")와 활성 예매(" + active + ")에 같은 좌석 연결");

        JDBCUtil jdbc = new JDBCUtil();
        try {
            dao.restoreCancelledAndExpiredSeats(jdbc);
            String status = seatStatus(jdbc, seatId);
            System.out.println("[INFO] 복원 쿼리 실행 후 좌석 상태: " + status);
            assertEquals("활성 예매가 있는 좌석은 복원되면 안 됩니다.", SeatStatus.HELD.getCode(), status);
            jdbc.rollback();
        } finally {
            jdbc.close();
        }
        System.out.println("[PASS] 활성 예매가 있는 좌석은 복원되지 않음");
    }

    @Test
    public void restore_bookedSeatOfCancelledBookingBecomesAvailable() throws Exception {
        System.out.println("\n[TEST] restoreCancelledAndExpiredSeats (취소 예매의 B 좌석) 시작");
        long cancelled = createBooking(BookingStatus.CANCELED.getCode());
        dao.insertBookingSeat(cancelled, seatId, 50000.0);
        setSeatStatus(seatId, SeatStatus.BOOKED.getCode());
        System.out.println("[INFO] 선행: 취소 예매(" + cancelled + ")에 좌석 연결, 좌석 상태 B");

        JDBCUtil jdbc = new JDBCUtil();
        try {
            dao.restoreCancelledAndExpiredSeats(jdbc);
            String status = seatStatus(jdbc, seatId);
            System.out.println("[INFO] 복원 쿼리 실행 후 좌석 상태: " + status);
            assertEquals("취소 예매의 B 좌석은 A로 복원되어야 합니다.", SeatStatus.AVAILABLE.getCode(), status);
            jdbc.rollback();
        } finally {
            jdbc.close();
        }
        System.out.println("[PASS] 취소 예매의 B 좌석이 A로 복원됨");
    }
    
    @Test
    public void restore_bookedSeatWithConfirmedBookingIsNotRestored() throws Exception {
        System.out.println("\n[TEST] restoreCancelledAndExpiredSeats (확정 예매 좌석 제외) 시작");
        long cancelled = createBooking(BookingStatus.CANCELED.getCode());
        long confirmed = createBooking(BookingStatus.CONFIRMED.getCode());
        dao.insertBookingSeat(cancelled, seatId, 50000.0);
        dao.insertBookingSeat(confirmed, seatId, 50000.0);
        setSeatStatus(seatId, SeatStatus.BOOKED.getCode());
        System.out.println("[INFO] 선행: 취소 예매(" + cancelled + ")와 확정 예매(" + confirmed
                + ")에 같은 좌석 연결, 좌석 상태 B");

        JDBCUtil jdbc = new JDBCUtil();
        try {
            dao.restoreCancelledAndExpiredSeats(jdbc);
            String status = seatStatus(jdbc, seatId);
            System.out.println("[INFO] 복원 쿼리 실행 후 좌석 상태: " + status);
            assertEquals("확정 예매가 있는 B 좌석은 복원되면 안 됩니다.", SeatStatus.BOOKED.getCode(), status);
            jdbc.rollback();
        } finally {
            jdbc.close();
        }
        System.out.println("[PASS] 확정 예매가 있는 B 좌석은 복원되지 않음");
    }

    // ===================== 헬퍼 =====================

    /** 시퀀스를 먼저 받아서 그 값으로 insert한다 */
    private long nextVal(JDBCUtil jdbc, String sequence) throws Exception {
        jdbc.setSqlAndParameters("SELECT " + sequence + ".NEXTVAL FROM DUAL", null);
        ResultSet rs = jdbc.executeQuery();
        rs.next();
        return rs.getLong(1);
    }

    private void execute(JDBCUtil jdbc, String sql, Object... params) throws Exception {
        jdbc.setSqlAndParameters(sql, params);
        jdbc.executeUpdate();
    }

    /** 테스트 전용 BOOKING (PAYMENT_ID는 NULL). tearDown에서 지울 수 있게 ID를 기록한다 */
    private long createBooking(String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            long id = nextVal(jdbc, "SEQ_BOOKING");
            bookingIds.add(id);   // insert 전에 기록: 중간에 실패해도 정리 시도
            execute(jdbc, "INSERT INTO BOOKING (BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID) "
                    + "VALUES (?, ?, SYSDATE, ?, NULL, ?)", id, status, memberId, performanceId);
            jdbc.commit();
            System.out.println("[INFO] 테스트 BOOKING 생성 (상태 " + status + ", PAYMENT_ID NULL): " + id);
            return id;
        } catch (Exception e) {
            jdbc.rollback();
            throw e;
        } finally {
            jdbc.close();
        }
    }

    private String seatStatus(long id) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            return seatStatus(jdbc, id);
        } finally {
            jdbc.close();
        }
    }

    /** 같은 연결(트랜잭션)에서 좌석 상태 조회 */
    private String seatStatus(JDBCUtil jdbc, long id) throws Exception {
        jdbc.setSqlAndParameters("SELECT SEAT_STATUS FROM SEAT WHERE SEAT_ID = ?", new Object[] { id });
        ResultSet rs = jdbc.executeQuery();
        assertTrue("좌석이 존재해야 합니다. id=" + id, rs.next());
        return rs.getString(1);
    }

    private void setSeatStatus(long id, String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            execute(jdbc, "UPDATE SEAT SET SEAT_STATUS = ? WHERE SEAT_ID = ?", status, id);
            jdbc.commit();
        } finally {
            jdbc.close();
        }
    }

    private long queryLong(String sql) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters(sql, null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) {
                long v = rs.getLong(1);
                return rs.wasNull() ? -1 : v;
            }
        } finally {
            jdbc.close();
        }
        return -1;
    }

    /** 정리 한 단계가 실패해도 다음 단계를 계속한다. 삭제 건수를 출력한다 */
    private void deleteQuietly(String label, String sql, long id) {
        if (id == 0) return;
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters(sql, new Object[] { id });
            int n = jdbc.executeUpdate();
            jdbc.commit();
            System.out.println("[TEARDOWN] " + label + " 삭제: " + n + "건");
        } catch (Exception e) {
            System.out.println("[TEARDOWN] " + label + " 정리 실패 (" + id + "): " + e.getMessage());
            try { jdbc.rollback(); } catch (Exception ignore) { }
        } finally {
            jdbc.close();
        }
    }
}
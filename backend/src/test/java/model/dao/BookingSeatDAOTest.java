package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * BookingSeatDAO 테스트 클래스
 * 공용 DB 오염 및 데이터 부재(무결성 제약조건)를 방지하기 위해 Venue부터 순서대로 직접
 * 생성하고 테스트 종료 후 안전하게 삭제합니다.
 *
 * BOOKING.PAYMENT_ID는 nullable이라 (BOOKING_PAYMENT_FK는 NULL이면 검사하지 않음)
 * 테스트 전용 BOOKING을 PAYMENT_ID = NULL로 직접 만들고 끝나면 지운다.
 * 그래서 공용 BOOKING/PAYMENT 행을 빌리지 않는다.
 */
public class BookingSeatDAOTest {

    private BookingSeatDAO bookingSeatDao;

    // 테스트 간 데이터 격리를 위한 식별자 ID 변수들
    private long testVenueId = 0;
    private long testSeatLayoutId = 0;
    private long testSectorId = 0;
    private long testSeatId = 0;
    private long testBookingId = 0;
    private long testBookingSeatId = 0;
    private long existingSeatGradeId = 0;
    private long existingPerformanceId = 0;
    private long existingMemberId = 0;

    @Before
    public void setUp() throws Exception {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        bookingSeatDao = new BookingSeatDAO();
        System.out.println("\n[SETUP] BookingSeatDAO 생성 완료, db.properties 확인됨");

        // 0. MEMBER 및 SEAT_GRADE의 같은 행에서 GRADE_ID, PERFORMANCE_ID 조회 (FK 충족)
        existingMemberId = findAnyId("MEMBER", "MEMBER_ID");
        assumeTrue("MEMBER 데이터가 필요합니다.", existingMemberId > 0);
        System.out.println("[SETUP] 기존 MEMBER_ID 사용: " + existingMemberId);

        long[] gp = findAnyGradeAndPerformance();
        assumeTrue("SEAT_GRADE 데이터가 필요합니다.", gp != null);
        existingSeatGradeId = gp[0];
        existingPerformanceId = gp[1];
        System.out.println("[SETUP] 기존 GRADE_ID=" + existingSeatGradeId
                + ", PERFORMANCE_ID=" + existingPerformanceId + " 사용");

        // 테스트 전용 BOOKING 생성 (PAYMENT_ID = NULL)
        testBookingId = insertTestBooking(existingMemberId, existingPerformanceId, "W");
        System.out.println("[SETUP] 테스트 BOOKING 생성 (상태 W, PAYMENT_ID NULL): " + testBookingId);

        testVenueId = insertTestVenue("테스트공연장_BS", 100);
        System.out.println("[SETUP] 테스트 VENUE 생성: " + testVenueId);
        testSeatLayoutId = insertTestSeatLayout(testVenueId, "테스트레이아웃_BS");
        System.out.println("[SETUP] 테스트 SEAT_LAYOUT 생성: " + testSeatLayoutId);

        // 구역: 조회한 GRADE_ID + PERFORMANCE_ID 쌍 사용
        testSectorId = insertTestSector(testSeatLayoutId, "테스트구역_BS", existingSeatGradeId, existingPerformanceId);
        System.out.println("[SETUP] 테스트 SECTOR 생성: " + testSectorId);

        // ROW_NUM은 VARCHAR2(10) 컬럼에 맞춰 10자 이내 문자열('A')로 입력
        testSeatId = insertTestSeat(testSeatLayoutId, testSectorId, "A", "A");
        System.out.println("[SETUP] 테스트 SEAT 생성 (상태 A): " + testSeatId);
    }

    @After
    public void tearDown() throws Exception {
        if (testSeatLayoutId == 0 && testSectorId == 0 && testSeatId == 0 && testBookingId == 0
                && testVenueId == 0) {
            System.out.println("[TEARDOWN] 정리할 데이터 없음");
            return;
        }

        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            // 외래키 제약조건 역순으로 안전하게 삭제
            // 테스트 전용 BOOKING에 연결된 BOOKING_SEAT는 BOOKING_ID 기준으로 모두 삭제
            if (testBookingId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM BOOKING_SEAT WHERE BOOKING_ID = ?",
                        new Object[] { testBookingId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] BOOKING_SEAT 삭제: " + n + "건");
            }
            if (testSeatId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT WHERE SEAT_ID = ?", new Object[] { testSeatId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] SEAT 삭제: " + n + "건");
            }
            if (testSectorId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SECTOR WHERE SECTOR_ID = ?", new Object[] { testSectorId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] SECTOR 삭제: " + n + "건");
            }
            if (testSeatLayoutId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_ID = ?",
                        new Object[] { testSeatLayoutId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] SEAT_LAYOUT 삭제: " + n + "건");
            }
            if (testVenueId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM VENUE WHERE VENUE_ID = ?", new Object[] { testVenueId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] VENUE 삭제: " + n + "건");
            }
            if (testBookingId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM BOOKING WHERE BOOKING_ID = ?",
                        new Object[] { testBookingId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] BOOKING 삭제: " + n + "건");
            }
            jdbcUtil.commit();
            System.out.println("[TEARDOWN] commit 완료");
        } catch (Exception e) {
            try {
                jdbcUtil.rollback();
            } catch (Exception ignore) {
            }
            System.out.println("[TEARDOWN] 실패, rollback: " + e.getMessage());
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }

    @Test
    public void testUpdateSeatToHeld() throws Exception {
        System.out.println("\n[TEST] updateSeatToHeld 시작");
        int result = bookingSeatDao.updateSeatToHeld(testSeatId);
        System.out.println("[INFO] 첫 번째 호출 결과: " + result);
        assertEquals("예매 가능('A') 상태의 좌석이 정상적으로 선점('H') 상태로 변경되어야 합니다.", 1, result);
        System.out.println("[PASS] A → H 변경 성공");

        int failResult = bookingSeatDao.updateSeatToHeld(testSeatId);
        System.out.println("[INFO] 두 번째 호출 결과: " + failResult);
        assertEquals("이미 선점된 좌석은 중복 선점할 수 없어야 합니다.", 0, failResult);
        System.out.println("[PASS] 이미 선점된 좌석은 0건 변경 (중복 선점 방지)");
    }

    @Test
    public void testInsertBookingSeat() throws Exception {
        System.out.println("\n[TEST] insertBookingSeat 시작");
        double price = 50000.0;
        int result = bookingSeatDao.insertBookingSeat(testBookingId, testSeatId, price);
        System.out.println("[INFO] insert 결과: " + result);
        assertEquals("예매-좌석 매핑 정보가 정상적으로 등록되어야 합니다.", 1, result);
        System.out.println("[PASS] BOOKING_SEAT 등록 성공 (bookingId=" + testBookingId
                + ", seatId=" + testSeatId + ", price=" + price + ")");

        testBookingSeatId = findLatestBookingSeatId(testBookingId);
        assertTrue(testBookingSeatId > 0);
        System.out.println("[PASS] 생성된 BOOKING_SEAT_ID: " + testBookingSeatId);
    }

    @Test
    public void testReleaseSeatsByBookingId() throws Exception {
        System.out.println("\n[TEST] releaseSeatsByBookingId 시작");
        int held = bookingSeatDao.updateSeatToHeld(testSeatId);
        System.out.println("[INFO] 선행: 좌석 홀드 결과 " + held);
        bookingSeatDao.insertBookingSeat(testBookingId, testSeatId, 50000.0);
        testBookingSeatId = findLatestBookingSeatId(testBookingId);
        System.out.println("[INFO] 선행: BOOKING_SEAT_ID " + testBookingSeatId + " 등록");

        int result = bookingSeatDao.releaseSeatsByBookingId(testBookingId);
        System.out.println("[INFO] 홀드 해제된 좌석 수: " + result);
        assertEquals("선점된 좌석이 예매 가능('A') 상태로 복원되어야 합니다.", 1, result);
        System.out.println("[PASS] H → A 복원 성공");
    }

    @Test
    public void testRestoreCancelledAndExpiredSeats() throws Exception {
        System.out.println("\n[TEST] restoreCancelledAndExpiredSeats 시작");
        int result = bookingSeatDao.restoreCancelledAndExpiredSeats();
        System.out.println("[INFO] 복원된 좌석 수: " + result + " (공용 DB에 commit됨)");
        assertTrue("만료 및 취소 좌석 복원 쿼리가 정상 실행되어야 합니다.", result >= 0);
        System.out.println("[PASS] 쿼리 정상 실행");
    }

    // --- 테스트 데이터 세팅용 헬퍼 메서드들 ---

    /** 테스트 전용 BOOKING 생성: PAYMENT_ID는 NULL (nullable 컬럼). 시퀀스를 먼저 받아서 ID를 정확히 얻는다 */
    private long insertTestBooking(long memberId, long performanceId, String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT SEQ_BOOKING.NEXTVAL FROM DUAL", null);
            ResultSet rs = jdbc.executeQuery();
            rs.next();
            long bookingId = rs.getLong(1);

            String sql = "INSERT INTO BOOKING (BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID) "
                    + "VALUES (?, ?, SYSDATE, ?, NULL, ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { bookingId, status, memberId, performanceId });
            jdbc.executeUpdate();
            jdbc.commit();
            return bookingId;
        } finally {
            jdbc.close();
        }
    }

    private long insertTestVenue(String name, int capacity) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO VENUE (VENUE_ID, VENUE_NAME, VENUE_CAPACITY) VALUES (SEQ_VENUE.NEXTVAL, ?, ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { name, capacity });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(VENUE_ID) FROM VENUE", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestSeatLayout(long venueId, String name) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO SEAT_LAYOUT (SEAT_LAYOUT_ID, VENUE_ID, SEAT_LAYOUT_NAME, IS_TEMPLATE) "
                    + "VALUES (SEQ_SEAT_LAYOUT.NEXTVAL, ?, ?, 'Y')";
            jdbc.setSqlAndParameters(sql, new Object[] { venueId, name });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE VENUE_ID = ?",
                    new Object[] { venueId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestSector(long layoutId, String sectorName, long seatGradeId, long perfId) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO SECTOR (SECTOR_ID, SEAT_LAYOUT_ID, SECTOR_NAME, GRADE_ID, PERFORMANCE_ID) "
                    + "VALUES (SEQ_SECTOR.NEXTVAL, ?, ?, ?, ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { layoutId, sectorName, seatGradeId, perfId });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(SECTOR_ID) FROM SECTOR WHERE SEAT_LAYOUT_ID = ?",
                    new Object[] { layoutId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestSeat(long layoutId, long sectorId, String rowNum, String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO SEAT (SEAT_ID, SEAT_LAYOUT_ID, SECTOR_ID, ROW_NUM, COLUMN_NUM, SEAT_NUMBER, SEAT_STATUS) "
                    + "VALUES (SEQ_SEAT.NEXTVAL, ?, ?, ?, 1, 'TEST-1', ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { layoutId, sectorId, rowNum, status });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(SEAT_ID) FROM SEAT WHERE SEAT_LAYOUT_ID = ?",
                    new Object[] { layoutId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    /** 테스트 전용 BOOKING에 연결된 가장 최근 BOOKING_SEAT_ID (다른 사람의 행과 섞이지 않게 BOOKING_ID로 한정) */
    private long findLatestBookingSeatId(long bookingId) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT MAX(BOOKING_SEAT_ID) FROM BOOKING_SEAT WHERE BOOKING_ID = ?",
                    new Object[] { bookingId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long[] findAnyGradeAndPerformance() throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT GRADE_ID, PERFORMANCE_ID FROM SEAT_GRADE WHERE ROWNUM = 1", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next())
                return new long[] { rs.getLong(1), rs.getLong(2) };
        } finally {
            jdbc.close();
        }
        return null;
    }

    private long findAnyId(String table, String idColumn) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT MIN(" + idColumn + ") FROM " + table, null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                return rs.wasNull() ? -1 : id;
            }
        } finally {
            jdbc.close();
        }
        return -1;
    }
}

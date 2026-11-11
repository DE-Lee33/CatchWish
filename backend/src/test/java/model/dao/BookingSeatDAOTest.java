package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * BookingSeatDAO 테스트 클래스
 * 공용 DB 오염 및 데이터 부재(무결성 제약조건)를 방지하기 위해 
 * Venue부터 순서대로 직접 생성하고 테스트 종료 후 안전하게 삭제합니다.
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

    @Before
    public void setUp() throws Exception {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        bookingSeatDao = new BookingSeatDAO();

        // 1. 테스트용 Venue(공연장) 생성 (부모)
        testVenueId = insertTestVenue("테스트공연장_BS");

        // 2. 테스트용 레이아웃 생성 (VENUE_ID 참조)
        testSeatLayoutId = insertTestSeatLayout(testVenueId, "테스트레이아웃_BS");

        // 3. 테스트용 구역 생성 (GRADE_ID, PERFORMANCE_ID는 임시로 1L 지정)
        testSectorId = insertTestSector(testSeatLayoutId, "테스트구역_BS", 1L, 1L);

        // 4. 테스트용 물리 좌석 생성 (초기 상태 'A' : 예매 가능)
        testSeatId = insertTestSeat(testSeatLayoutId, testSectorId, "A");

        // 5. 테스트용 예매(Booking) 레코드 생성 (상태 'W' : 선점/대기)
        testBookingId = insertTestBooking(1L, 1L, "W");
    }

    @After
    public void tearDown() throws Exception {
        if (testSeatLayoutId == 0 && testSectorId == 0 && testSeatId == 0 && testBookingId == 0 && testBookingSeatId == 0 && testVenueId == 0) {
            return;
        }

        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            // 외래키 제약조건 역순으로 안전하게 삭제 (BookingSeat -> Booking -> Seat -> Sector -> SeatLayout -> Venue)
            if (testBookingSeatId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM BOOKING_SEAT WHERE BOOKING_SEAT_ID = ?", new Object[] { testBookingSeatId });
                jdbcUtil.executeUpdate();
            }
            if (testBookingId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM BOOKING WHERE BOOKING_ID = ?", new Object[] { testBookingId });
                jdbcUtil.executeUpdate();
            }
            if (testSeatId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT WHERE SEAT_ID = ?", new Object[] { testSeatId });
                jdbcUtil.executeUpdate();
            }
            if (testSectorId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SECTOR WHERE SECTOR_ID = ?", new Object[] { testSectorId });
                jdbcUtil.executeUpdate();
            }
            if (testSeatLayoutId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_ID = ?", new Object[] { testSeatLayoutId });
                jdbcUtil.executeUpdate();
            }
            if (testVenueId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM VENUE WHERE VENUE_ID = ?", new Object[] { testVenueId });
                jdbcUtil.executeUpdate();
            }
            jdbcUtil.commit();
        } catch (Exception e) {
            try {
                jdbcUtil.rollback();
            } catch (Exception ignore) {}
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }

    @Test
    public void testUpdateSeatToHeld() throws Exception {
        int result = bookingSeatDao.updateSeatToHeld(testSeatId);
        assertEquals("예매 가능('A') 상태의 좌석이 정상적으로 선점('H') 상태로 변경되어야 합니다.", 1, result);

        int failResult = bookingSeatDao.updateSeatToHeld(testSeatId);
        assertEquals("이미 선점된 좌석은 중복 선점할 수 없어야 합니다.", 0, failResult);
    }

    @Test
    public void testInsertBookingSeat() throws Exception {
        double price = 50000.0;
        int result = bookingSeatDao.insertBookingSeat(testBookingId, testSeatId, price);
        assertEquals("예매-좌석 매핑 정보가 정상적으로 등록되어야 합니다.", 1, result);

        testBookingSeatId = findLatestBookingSeatId();
        assertTrue(testBookingSeatId > 0);
    }

    @Test
    public void testReleaseSeatsByBookingId() throws Exception {
        bookingSeatDao.updateSeatToHeld(testSeatId);
        bookingSeatDao.insertBookingSeat(testBookingId, testSeatId, 50000.0);
        testBookingSeatId = findLatestBookingSeatId();

        int result = bookingSeatDao.releaseSeatsByBookingId(testBookingId);
        assertEquals("선점된 좌석이 예매 가능('A') 상태로 복원되어야 합니다.", 1, result);
    }

    @Test
    public void testRestoreCancelledAndExpiredSeats() throws Exception {
        int result = bookingSeatDao.restoreCancelledAndExpiredSeats();
        assertTrue("만료 및 취소 좌석 복원 쿼리가 정상 실행되어야 합니다.", result >= 0);
    }

    // --- 테스트 데이터 세팅용 헬퍼 메서드들 ---

    private long insertTestVenue(String name) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO VENUE (VENUE_ID, VENUE_NAME) VALUES (SEQ_VENUE.NEXTVAL, ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { name });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(VENUE_ID) FROM VENUE", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
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
            
            jdbc.setSqlAndParameters("SELECT MAX(SEAT_LAYOUT_ID) FROM SEAT_LAYOUT WHERE VENUE_ID = ?", new Object[] { venueId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestSector(long layoutId, String sectorName, long gradeId, long perfId) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO SECTOR (SECTOR_ID, SEAT_LAYOUT_ID, SECTOR_NAME, GRADE_ID, PERFORMANCE_ID) "
                       + "VALUES (SEQ_SECTOR.NEXTVAL, ?, ?, ?, ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { layoutId, sectorName, gradeId, perfId });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(SECTOR_ID) FROM SECTOR WHERE SEAT_LAYOUT_ID = ?", new Object[] { layoutId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestSeat(long layoutId, long sectorId, String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO SEAT (SEAT_ID, SEAT_LAYOUT_ID, SECTOR_ID, ROW_NUM, COLUMN_NUM, SEAT_NUMBER, SEAT_STATUS) "
                       + "VALUES (SEQ_SEAT.NEXTVAL, ?, ?, 1, 1, 'TEST-1', ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { layoutId, sectorId, status });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(SEAT_ID) FROM SEAT WHERE SEAT_LAYOUT_ID = ?", new Object[] { layoutId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestBooking(long memberId, long performanceId, String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO BOOKING (BOOKING_ID, MEMBER_ID, PERFORMANCE_ID, BOOKING_STATUS, CREATED_AT) "
                       + "VALUES (SEQ_BOOKING.NEXTVAL, ?, ?, ?, SYSDATE)";
            jdbc.setSqlAndParameters(sql, new Object[] { memberId, performanceId, status });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(BOOKING_ID) FROM BOOKING", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long findLatestBookingSeatId() throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT MAX(BOOKING_SEAT_ID) FROM BOOKING_SEAT", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }
}
package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * BookingSeatDAO 테스트 클래스
 */
public class BookingSeatDAOTest {

    private BookingSeatDAO bookingSeatDao;

    // 테스트 간 데이터 격리를 위한 식별자 변수들
    private long testSeatId = 0;
    private long testBookingId = 0;
    private long testBookingSeatId = 0;

    @Before
    public void setUp() throws Exception {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        bookingSeatDao = new BookingSeatDAO();

        // 1. 테스트용 물리 좌석 생성 (초기 상태 'A' : 예매 가능)
        testSeatId = insertTestSeat("A");

        // 2. 테스트용 예매(Booking) 레코드 생성 (상태 'W' : 선점/대기)
        testBookingId = insertTestBooking("W");
    }

    @After
    public void tearDown() throws Exception {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            // 외래키 제약조건을 고려하여 자식 테이블(BOOKING_SEAT)부터 부모 테이블 순으로 삭제
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
            jdbcUtil.commit();
        } catch (Exception e) {
            jdbcUtil.rollback();
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }

    @Test
    public void testUpdateSeatToHeld() throws Exception {
        // 'A' 상태인 좌석을 'H'(선점중)로 변경
        int result = bookingSeatDao.updateSeatToHeld(testSeatId);
        assertEquals("예매 가능('A') 상태의 좌석이 정상적으로 선점('H') 상태로 변경되어야 합니다.", 1, result);

        // 이미 'H' 상태인 좌석을 한 번 더 선점 시도 시 중복 방지로 인해 0이 반환되어야 함
        int failResult = bookingSeatDao.updateSeatToHeld(testSeatId);
        assertEquals("이미 선점된 좌석은 중복 선점할 수 없어야 합니다.", 0, failResult);
    }

    @Test
    public void testInsertBookingSeat() throws Exception {
        double price = 50000.0;
        int result = bookingSeatDao.insertBookingSeat(testBookingId, testSeatId, price);
        assertEquals("예매-좌석 매핑 정보가 정상적으로 등록되어야 합니다.", 1, result);

        // 등록된 ID 저장 (tearDown에서 삭제하기 위함)
        testBookingSeatId = findLatestBookingSeatId();
        assertTrue(testBookingSeatId > 0);
    }

    @Test
    public void testReleaseSeatsByBookingId() throws Exception {
        // 선행 데이터: 좌석 선점 및 매핑 정보 등록
        bookingSeatDao.updateSeatToHeld(testSeatId);
        bookingSeatDao.insertBookingSeat(testBookingId, testSeatId, 50000.0);
        testBookingSeatId = findLatestBookingSeatId();

        // 결제 실패/취소 시 특정 예매 ID의 좌석을 'A'로 복원
        int result = bookingSeatDao.releaseSeatsByBookingId(testBookingId);
        assertEquals("선점된 좌석이 예매 가능('A') 상태로 복원되어야 합니다.", 1, result);
    }

    @Test
    public void testRestoreCancelledAndExpiredSeats() throws Exception {
        // 취소 상태('X')이거나 만료된 선점 예매 건의 좌석 복원 로직 검증
        int result = bookingSeatDao.restoreCancelledAndExpiredSeats();
        // 실행 결과 자체에 예외가 발생하지 않고 정상 수행되는지 확인
        assertTrue("만료 및 취소 좌석 복원 쿼리가 정상 실행되어야 합니다.", result >= 0);
    }

    // --- 테스트 데이터 세팅용 헬퍼 메서드들 ---

    private long insertTestSeat(String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            // 테스트용 레이아웃 및 구역 ID는 기존 DB에 존재하는 임의의 값(예: 1L)을 사용하거나 레이아웃부터 생성 필요
            String sql = "INSERT INTO SEAT (SEAT_ID, SEAT_LAYOUT_ID, SECTOR_ID, ROW_NUM, COLUMN_NUM, SEAT_NUMBER, SEAT_STATUS) "
                       + "VALUES (SEQ_SEAT.NEXTVAL, 1, 1, 1, 1, 'TEST-1', ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { status });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(SEAT_ID) FROM SEAT", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestBooking(String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            // BOOKING 테이블 스키마에 맞춰 member_id, performance_id 등 필수 컬럼 반영 (기본값 설정 가정)
            String sql = "INSERT INTO BOOKING (BOOKING_ID, MEMBER_ID, PERFORMANCE_ID, BOOKING_STATUS, CREATED_AT) "
                       + "VALUES (SEQ_BOOKING.NEXTVAL, 1, 1, ?, SYSDATE)";
            jdbc.setSqlAndParameters(sql, new Object[] { status });
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
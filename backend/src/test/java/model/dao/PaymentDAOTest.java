package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.dto.PaymentRequestDTO;

/**
 * PaymentDAO 테스트 클래스
 */
public class PaymentDAOTest {

    private PaymentDAO paymentDao;

    // 테스트 데이터 식별자 변수들
    private long testBookingId = 0;
    private long testPaymentId = 0;

    @Before
    public void setUp() throws Exception {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        paymentDao = new PaymentDAO();

        // 결제(Payment)의 외래키(BOOKING_ID1) 충족을 위한 테스트용 예매(Booking) 데이터 생성
        testBookingId = insertTestBooking(1L, 1L, "W");
    }

    @After
    public void tearDown() throws Exception {
        if (testBookingId == 0) {
            return;
        }

        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            // 외래키 제약조건에 따라 자식 테이블(PAYMENT) 먼저 삭제 후 부모 테이블(BOOKING) 삭제
            jdbcUtil.setSqlAndParameters("DELETE FROM PAYMENT WHERE BOOKING_ID1 = ?", new Object[] { testBookingId });
            jdbcUtil.executeUpdate();

            jdbcUtil.setSqlAndParameters("DELETE FROM BOOKING WHERE BOOKING_ID = ?", new Object[] { testBookingId });
            jdbcUtil.executeUpdate();

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
    public void testInsertPayment() throws Exception {
        // 결제 요청 DTO 생성 (bookingId, amount, method, transactionId)
        PaymentRequestDTO request = new PaymentRequestDTO(testBookingId, 50000.0, "CARD", "TX_TEST_12345");

        int result = paymentDao.insertPayment(request);
        assertEquals("결제 내역 등록이 성공해야 합니다.", 1, result);

        testPaymentId = findLatestPaymentId();
        assertTrue(testPaymentId > 0);
    }

    @Test
    public void testUpdatePaymentSuccess() throws Exception {
        // 선행: 결제 대기 상태로 등록
        PaymentRequestDTO request = new PaymentRequestDTO(testBookingId, 50000.0, "CARD", "TX_BEFORE");
        paymentDao.insertPayment(request);

        // 결제 성공 처리 테스트 (상태 'C', 거래 번호 갱신)
        String newTransactionId = "TX_SUCCESS_99999";
        int result = paymentDao.updatePaymentSuccess(testBookingId, newTransactionId);
        assertEquals("결제 성공 상태('C') 및 거래 번호 업데이트가 반영되어야 합니다.", 1, result);
    }

    @Test
    public void testUpdatePaymentStatus() throws Exception {
        // 선행: 결제 대기 상태로 등록
        PaymentRequestDTO request = new PaymentRequestDTO(testBookingId, 50000.0, "CARD", "TX_FAIL_TEST");
        paymentDao.insertPayment(request);

        // 결제 실패 처리 테스트 (상태 'F')
        int result = paymentDao.updatePaymentStatus(testBookingId, "F");
        assertEquals("결제 상태가 실패('F')로 변경되어야 합니다.", 1, result);
    }

    // --- 테스트 데이터 세팅용 헬퍼 메서드들 ---

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

    private long findLatestPaymentId() throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT MAX(PAYMENT_ID) FROM PAYMENT", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }
}
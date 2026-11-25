package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.domain.enums.PaymentStatus;
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
        System.out.println("\n[SETUP] PaymentDAO 생성 완료, db.properties 확인됨");

        // BOOKING의 FK(PERFORMANCE, MEMBER)를 충족하기 위해 실제 존재하는 ID 조회
        long existingPerformanceId = findAnyId("PERFORMANCE", "PERFORMANCE_ID");
        long existingMemberId = findAnyId("MEMBER", "MEMBER_ID");
        assumeTrue("PERFORMANCE 데이터가 필요합니다.", existingPerformanceId > 0);
        assumeTrue("MEMBER 데이터가 필요합니다.", existingMemberId > 0);
        System.out.println("[SETUP] 기존 PERFORMANCE_ID=" + existingPerformanceId
                + ", MEMBER_ID=" + existingMemberId + " 사용");

        // 결제(Payment)의 외래키(BOOKING_ID) 충족을 위한 테스트용 예매(Booking) 데이터 생성
        testBookingId = insertTestBooking(existingMemberId, existingPerformanceId, "W");
        System.out.println("[SETUP] 테스트 BOOKING 생성 (상태 W): " + testBookingId);
    }

    @After
    public void tearDown() throws Exception {
        if (testBookingId == 0) {
            System.out.println("[TEARDOWN] 정리할 데이터 없음");
            return;
        }

        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            // 외래키 제약조건에 따라 자식 테이블(PAYMENT) 먼저 삭제 후 부모 테이블(BOOKING) 삭제
            jdbcUtil.setSqlAndParameters("DELETE FROM PAYMENT WHERE BOOKING_ID = ?", new Object[] { testBookingId });
            int p = jdbcUtil.executeUpdate();
            System.out.println("[TEARDOWN] PAYMENT 삭제: " + p + "건");

            jdbcUtil.setSqlAndParameters("DELETE FROM BOOKING WHERE BOOKING_ID = ?", new Object[] { testBookingId });
            int b = jdbcUtil.executeUpdate();
            System.out.println("[TEARDOWN] BOOKING 삭제: " + b + "건");

            jdbcUtil.commit();
            System.out.println("[TEARDOWN] commit 완료");
        } catch (Exception e) {
            try {
                jdbcUtil.rollback();
            } catch (Exception ignore) {}
            System.out.println("[TEARDOWN] 실패, rollback: " + e.getMessage());
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }

    @Test
    public void testInsertPayment() throws Exception {
        System.out.println("\n[TEST] insertPayment 시작");
        // 결제 요청 DTO 생성 (bookingId, amount, method, transactionId)
        PaymentRequestDTO request = new PaymentRequestDTO(testBookingId, 50000.0, "CARD", "TX_TEST_12345");

        int result = paymentDao.insertPayment(request);
        System.out.println("[INFO] insert 결과: " + result);
        assertEquals("결제 내역 등록이 성공해야 합니다.", 1, result);
        System.out.println("[PASS] PAYMENT 등록 성공 (bookingId=" + testBookingId + ", 금액=50000.0, CARD)");

        testPaymentId = findLatestPaymentId();
        assertTrue(testPaymentId > 0);
        System.out.println("[PASS] 생성된 PAYMENT_ID: " + testPaymentId);
    }

    @Test
    public void testUpdatePaymentSuccess() throws Exception {
        System.out.println("\n[TEST] updatePaymentSuccess 시작");
        // 선행: 결제 대기 상태로 등록
        PaymentRequestDTO request = new PaymentRequestDTO(testBookingId, 50000.0, "CARD", "TX_BEFORE");
        int inserted = paymentDao.insertPayment(request);
        System.out.println("[INFO] 선행: 결제 대기 등록 결과 " + inserted);

        // 결제 성공 처리 테스트 (상태 'C', 거래 번호 갱신)
        String newTransactionId = "TX_SUCCESS_99999";
        int result = paymentDao.updatePaymentSuccess(testBookingId, newTransactionId);
        System.out.println("[INFO] 결제 성공 처리 결과: " + result);
        assertEquals("결제 성공 상태('C') 및 거래 번호 업데이트가 반영되어야 합니다.", 1, result);
        System.out.println("[PASS] 상태 C, 거래번호 " + newTransactionId + " 반영");
    }

    @Test
    public void testUpdatePaymentStatus() throws Exception {
        System.out.println("\n[TEST] updatePaymentStatus 시작");
        // 선행: 결제 대기 상태로 등록
        PaymentRequestDTO request = new PaymentRequestDTO(testBookingId, 50000.0, "CARD", "TX_FAIL_TEST");
        int inserted = paymentDao.insertPayment(request);
        System.out.println("[INFO] 선행: 결제 대기 등록 결과 " + inserted);

        // 결제 실패 처리 테스트 (상태 'F')
        int result = paymentDao.updatePaymentStatus(testBookingId, PaymentStatus.FAILED);
        System.out.println("[INFO] 상태 변경 결과: " + result);
        assertEquals("결제 상태가 실패('F')로 변경되어야 합니다.", 1, result);
        System.out.println("[PASS] 결제 상태 F(FAILED)로 변경 성공");
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

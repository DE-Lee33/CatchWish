/*
 * [변경 요약] PaymentDAO  (표시: [수정] [추가] [삭제])
 * [삭제] private JDBCUtil jdbcUtil 필드, 생성자 (요청 간 Connection 공유 방지)
 * [추가] 트랜잭션 참여용 오버로드 3개: insertPayment, updatePaymentSuccess, updatePaymentStatus
 * [수정] 위 3개의 단독용 메서드는 참여용을 호출하고 commit
 * [삭제] executeChange (단독용이 직접 처리하므로 불필요)
 * [수정] rollbackQuietly: JDBCUtil을 파라미터로 받음
 */
package model.dao;

import java.sql.SQLException;

import model.domain.enums.PaymentStatus;
import org.springframework.stereotype.Repository;
import model.dto.PaymentRequestDTO;

@Repository
public class PaymentDAO {
    // [삭제] private JDBCUtil jdbcUtil = null;
    
    // [삭제] public PaymentDAO() {
    // [삭제]     jdbcUtil = new JDBCUtil();
    // [삭제] }
    
    // [수정] 단독용: 참여용을 호출하고 commit
    public int insertPayment(PaymentRequestDTO request) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = insertPayment(jdbcUtil, request);
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("결제 등록 실패", ex);
        } finally {
            jdbcUtil.close();
        }
    }

    // [추가] 트랜잭션 참여용 (commit/rollback/close는 호출한 쪽에서)
    public int insertPayment(JDBCUtil jdbcUtil, PaymentRequestDTO request) throws Exception {
        String sql = "INSERT INTO payment(payment_id, booking_id, payment_amount, payment_method, payment_status, paid_at, transaction_id) "
                   + "VALUES (SEQ_PAYMENT.NEXTVAL, ?, ?, ?, ?, SYSDATE, ?)";
        
        Object[] params = new Object[] {
            request.getBookingId(),
            request.getPaymentAmount(),
            request.getPaymentMethod(),
            PaymentStatus.PENDING.getCode(),
            request.getTransactionId()
        };
        
        jdbcUtil.setSqlAndParameters(sql, params);
        return jdbcUtil.executeUpdate();
    }
    
    // [수정] 단독용
    public int updatePaymentSuccess(long bookingId, String transactionId) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updatePaymentSuccess(jdbcUtil, bookingId, transactionId);
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("결제 성공 처리 실패", ex);
        } finally {
            jdbcUtil.close();
        }
    }

    // [추가] 트랜잭션 참여용. 0이 반환되면 해당 예매의 결제 내역이 없음 → 호출한 쪽에서 롤백
    public int updatePaymentSuccess(JDBCUtil jdbcUtil, long bookingId, String transactionId) throws Exception {
        String sql = "UPDATE payment "
                   + "SET payment_status = ?, paid_at = SYSDATE, transaction_id = ? "
                   + "WHERE booking_id = ?";
        
        jdbcUtil.setSqlAndParameters(sql, new Object[] {
            PaymentStatus.COMPLETED.getCode(),
            transactionId,
            bookingId
        });
      
        return jdbcUtil.executeUpdate();
    }
    
    public int updatePaymentStatus(JDBCUtil jdbcUtil, long bookingId, PaymentStatus paymentStatus) throws Exception {
        String sql = "UPDATE payment "
                   + "SET payment_status = ? "
                   + "WHERE booking_id = ?";

        jdbcUtil.setSqlAndParameters(sql, new Object[] {
            paymentStatus.getCode(),
            bookingId
        });
        
        return jdbcUtil.executeUpdate();
    }
    
    // [수정] 단독용
    public int updatePaymentStatus(long bookingId, PaymentStatus paymentStatus) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updatePaymentStatus(jdbcUtil, bookingId, paymentStatus);
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("결제 상태 변경 실패", ex);
        } finally {
            jdbcUtil.close();
        }
    }
    
    // [삭제] private int executeChange(String sql, Object[] params) throws SQLException {
    // [삭제]     try {
    // [삭제]         jdbcUtil.setSqlAndParameters(sql, params);
    // [삭제]         int result = jdbcUtil.executeUpdate();
    // [삭제]         jdbcUtil.commit();
    // [삭제]         return result;
    // [삭제]     }
    // [삭제]     catch (Exception ex) {
    // [삭제]         rollbackQuietly();
    // [삭제]         throw toSQLException("결제 처리 실패", ex);
    // [삭제]     }
    // [삭제]     finally {
    // [삭제]         jdbcUtil.close();
    // [삭제]     }
    // [삭제] }
    
    private void rollbackQuietly(JDBCUtil jdbcUtil) {   // [수정] 파라미터 추가
        try {
            jdbcUtil.rollback();
        } 
        catch (Exception ignore) {
        }
    }
    
    private SQLException toSQLException(String message, Exception ex) {
        if (ex instanceof SQLException) {
            return (SQLException) ex;
        }
        return new SQLException(message, ex);
    }
}
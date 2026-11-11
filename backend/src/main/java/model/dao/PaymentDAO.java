package model.dao;

import java.sql.SQLException;

import model.dto.PaymentRequestDTO;

public class PaymentDAO {
    private JDBCUtil jdbcUtil = null;
    
    public PaymentDAO() {
        jdbcUtil = new JDBCUtil();
    }
    
    public int insertPayment(PaymentRequestDTO request) throws SQLException {
        String sql = "INSERT INTO payment(payment_id, booking_id1, payment_amount, payment_method, payment_status, paid_at, transaction_id) "
                   + "VALUES (SEQ_PAYMENT.NEXTVAL, ?, ?, ?, 'P', SYSDATE, ?)";
        
        Object[] params = new Object[] {
            request.getBookingId(),
            request.getPaymentAmount(),
            request.getPaymentMethod(),
            request.getTransactionId()
        };
        
        return executeChange(sql, params);
    }
    
    public int updatePaymentSuccess(long bookingId, String transactionId) throws SQLException {
        String sql = "UPDATE payment "
                   + "SET payment_status = 'C', paid_at = SYSDATE, transaction_id = ? "
                   + "WHERE booking_id1 = ?";
        
        return executeChange(sql, new Object[] { transactionId, bookingId });
    }
    
    public int updatePaymentStatus(long bookingId, String paymentStatus) throws SQLException {
        String sql = "UPDATE payment "
                   + "SET payment_status = ? "
                   + "WHERE booking_id1 = ?";
        
        return executeChange(sql, new Object[] { paymentStatus, bookingId });
    }
    
    private int executeChange(String sql, Object[] params) throws SQLException {
        jdbcUtil.setSqlAndParameters(sql, params);
        
        try {
            int result = jdbcUtil.executeUpdate();
            
            jdbcUtil.commit();
            
            return result;
        } 
        catch (Exception ex) {
            rollbackQuietly();
            
            throw toSQLException("결제 처리 실패", ex);
        } 
        finally {
            jdbcUtil.close();
        }
    }
    
    private void rollbackQuietly() {
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

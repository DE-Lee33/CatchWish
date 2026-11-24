package model.dao;

import java.sql.SQLException;
import model.domain.enums.BookingStatus;
import model.domain.enums.SeatStatus;

public class BookingSeatDAO {
    private JDBCUtil jdbcUtil = null;
    
    public BookingSeatDAO() {
        jdbcUtil = new JDBCUtil();
    }
    
    public int restoreCancelledAndExpiredSeats() throws SQLException {
        String sql = "UPDATE seat "
                   + "SET seat_status = ? "
                   + "WHERE seat_id IN ( "
                   + "    SELECT bs.seat_id "
                   + "    FROM booking_seat bs JOIN booking b ON bs.booking_id = b.booking_id "
                   + "    WHERE b.booking_status = ? OR (b.booking_status = ? AND b.created_at < SYSDATE - (10 / (24 * 60))) "
                   + ")";
        
        return executeChange(sql, new Object[] { SeatStatus.AVAILABLE.getCode(), BookingStatus.CANCELED.getCode(),
                BookingStatus.WAITING_PAYMENT.getCode() });
    }
    
    public int releaseSeatsByBookingId(long bookingId) throws SQLException {
        String sql = "UPDATE seat "
                   + "SET seat_status = ? "
                   + "WHERE seat_id IN ( "
                   + "    SELECT seat_id "
                   + "    FROM booking_seat "
                   + "    WHERE booking_id = ? "
                   + ") AND seat_status = ?";
        
        return executeChange(sql, new Object[] { SeatStatus.AVAILABLE.getCode(), bookingId, SeatStatus.HELD.getCode() });
    }
    
    public int insertBookingSeat(long bookingId, long seatId, double price) throws SQLException {
        String sql = "INSERT INTO booking_seat(booking_seat_id, booking_id, seat_id, price_at_booking) "
                   + "VALUES (SEQ_BOOKING_SEAT.NEXTVAL, ?, ?, ?)";
        
        return executeChange(sql, new Object[] { bookingId, seatId, price });
    }
    
    public int updateSeatToHeld(long seatId) throws SQLException {
        String sql = "UPDATE seat "
                   + "SET seat_status = ? "
                   + "WHERE seat_id = ? AND seat_status = ?";
        
        return executeChange(sql, new Object[] { SeatStatus.HELD.getCode(), seatId, SeatStatus.AVAILABLE.getCode() });
    }
    
    private int executeChange(String sql, Object[] params) throws SQLException {
        try {
            jdbcUtil.setSqlAndParameters(sql, params);
            
            int result = jdbcUtil.executeUpdate();
            
            jdbcUtil.commit();
            
            return result;
        } 
        catch (Exception ex) {
            rollbackQuietly();
            
            throw toSQLException("예매-좌석 처리 실패", ex);
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
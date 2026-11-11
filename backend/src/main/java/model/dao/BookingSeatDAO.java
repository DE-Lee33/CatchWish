package model.dao;

import java.sql.SQLException;

public class BookingSeatDAO {
    private JDBCUtil jdbcUtil = null;
    
    public BookingSeatDAO() {
        jdbcUtil = new JDBCUtil();
    }
    
    public int restoreCancelledAndExpiredSeats() throws SQLException {
        String sql = "UPDATE seat "
                   + "SET seat_status = 'A' "
                   + "WHERE seat_id IN ( "
                   + "    SELECT bs.seat_id "
                   + "    FROM booking_seat bs JOIN booking b ON bs.booking_id = b.booking_id "
                   + "    WHERE b.booking_status = 'X' OR (b.booking_status = 'W' AND b.created_at < SYSDATE - (10 / (24 * 60))) "
                   + ")";
        
        return executeChange(sql, null);
    }
    
    public int releaseSeatsByBookingId(long bookingId) throws SQLException {
        String sql = "UPDATE seat "
                   + "SET seat_status = 'A' "
                   + "WHERE seat_id IN ( "
                   + "    SELECT seat_id "
                   + "    FROM booking_seat "
                   + "    WHERE booking_id = ? "
                   + ") AND seat_status = 'H'";
        
        return executeChange(sql, new Object[] { bookingId });
    }
    
    public int insertBookingSeat(long bookingId, long seatId, double price) throws SQLException {
        String sql = "INSERT INTO booking_seat(booking_seat_id, booking_id, seat_id, price_at_booking) "
                   + "VALUES (SEQ_BOOKING_SEAT.NEXTVAL, ?, ?, ?)";
        
        return executeChange(sql, new Object[] { bookingId, seatId, price });
    }
    
    public int updateSeatToHeld(long seatId) throws SQLException {
        String sql = "UPDATE seat "
                   + "SET seat_status = 'H' "
                   + "WHERE seat_id = ? AND seat_status = 'A'";
        
        return executeChange(sql, new Object[] { seatId });
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

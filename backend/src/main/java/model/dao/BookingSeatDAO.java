package model.dao;

import java.sql.SQLException;
import model.domain.enums.BookingStatus;
import model.domain.enums.SeatStatus;

import org.springframework.stereotype.Repository;

@Repository
public class BookingSeatDAO {
    /** 단독용 */
    public int restoreCancelledAndExpiredSeats() throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = restoreCancelledAndExpiredSeats(jdbcUtil);
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("취소/만료 좌석 복원 실패", ex);
        } finally {
            jdbcUtil.close();
        }
    }
    
    /** 트랜잭션 참여용 (commit/rollback/close는 호출한 쪽에서) */
    public int restoreCancelledAndExpiredSeats(JDBCUtil jdbcUtil) throws Exception {
        String sql = "UPDATE seat s "
                   + "SET s.seat_status = ? "
                   + "WHERE s.seat_status IN (?, ?) "
                   + "AND s.seat_id IN ( "
                   + "    SELECT bs.seat_id "
                   + "    FROM booking_seat bs JOIN booking b ON bs.booking_id = b.booking_id "
                   + "    WHERE b.booking_status = ? "
                   + "       OR (b.booking_status = ? AND b.created_at < SYSDATE - (10 / (24 * 60))) "
                   + ") "
                   + "AND NOT EXISTS ( "
                   + "    SELECT 1 "
                   + "    FROM booking_seat bs2 JOIN booking b2 ON bs2.booking_id = b2.booking_id "
                   + "    WHERE bs2.seat_id = s.seat_id "
                   + "      AND (b2.booking_status = ? "
                   + "        OR (b2.booking_status = ? AND b2.created_at >= SYSDATE - (10 / (24 * 60)))) "
                   + ")";

        jdbcUtil.setSqlAndParameters(sql, new Object[] {
                SeatStatus.AVAILABLE.getCode(),
                SeatStatus.HELD.getCode(), SeatStatus.CANCEL_PENDING.getCode(),
                BookingStatus.CANCELED.getCode(), BookingStatus.WAITING_PAYMENT.getCode(),
                BookingStatus.CONFIRMED.getCode(), BookingStatus.WAITING_PAYMENT.getCode() });
        return jdbcUtil.executeUpdate();
    }

    public int releaseSeatsByBookingId(long bookingId) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = releaseSeatsByBookingId(jdbcUtil, bookingId);
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("좌석 홀드 해제 실패", ex);
        } finally {
            jdbcUtil.close();
        }
    }

    public int releaseSeatsByBookingId(JDBCUtil jdbcUtil, long bookingId) throws Exception {
        String sql = "UPDATE seat "
                   + "SET seat_status = ? "
                   + "WHERE seat_id IN ( "
                   + "    SELECT seat_id "
                   + "    FROM booking_seat "
                   + "    WHERE booking_id = ? "
                   + ") AND seat_status = ?";
        
        jdbcUtil.setSqlAndParameters(sql, new Object[] { SeatStatus.AVAILABLE.getCode(), bookingId, SeatStatus.HELD.getCode() });
        
        return jdbcUtil.executeUpdate();
    }

    public int insertBookingSeat(long bookingId, long seatId, double price) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = insertBookingSeat(jdbcUtil, bookingId, seatId, price);
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("예매-좌석 등록 실패", ex);
        } finally {
            jdbcUtil.close();
        }
    }

    public int insertBookingSeat(JDBCUtil jdbcUtil, long bookingId, long seatId, double price) throws Exception {
        String sql = "INSERT INTO booking_seat(booking_seat_id, booking_id, seat_id, price_at_booking) "
                   + "VALUES (SEQ_BOOKING_SEAT.NEXTVAL, ?, ?, ?)";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { bookingId, seatId, price });
        return jdbcUtil.executeUpdate();
    }

    public int updateSeatToHeld(long seatId) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateSeatToHeld(jdbcUtil, seatId);
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("좌석 홀드 실패", ex);
        } finally {
            jdbcUtil.close();
        }
    }

    public int updateSeatToHeld(JDBCUtil jdbcUtil, long seatId) throws Exception {
        String sql = "UPDATE seat "
                   + "SET seat_status = ? "
                   + "WHERE seat_id = ? AND seat_status = ?";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { SeatStatus.HELD.getCode(), seatId, SeatStatus.AVAILABLE.getCode() });
        
        return jdbcUtil.executeUpdate();
    }

    private void rollbackQuietly(JDBCUtil jdbcUtil) {   
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
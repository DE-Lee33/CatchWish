/*
 * [변경 요약] BookingSeatDAO  (표시: [수정] [추가] [삭제])
 * [삭제] private JDBCUtil jdbcUtil 필드, 생성자 (요청 간 Connection 공유 방지)
 * [추가] 트랜잭션 참여용 오버로드 3개: releaseSeatsByBookingId, insertBookingSeat, updateSeatToHeld
 * [수정] 위 3개의 단독용 메서드는 참여용을 호출하고 commit
 * [수정] executeChange, rollbackQuietly: 필드 대신 지역/파라미터 JDBCUtil 사용
 * 변경 없음: restoreCancelledAndExpiredSeats
 */
package model.dao;

import java.sql.SQLException;

import org.springframework.stereotype.Repository;

@Repository
public class BookingSeatDAO {
    // [삭제] private JDBCUtil jdbcUtil = null;

    // [삭제] public BookingSeatDAO() {
    // [삭제]     jdbcUtil = new JDBCUtil();
    // [삭제] }

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

    // [수정] 단독용: 참여용을 호출하고 commit
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

    // [추가] 트랜잭션 참여용 (commit/rollback/close는 호출한 쪽에서)
    public int releaseSeatsByBookingId(JDBCUtil jdbcUtil, long bookingId) throws Exception {
        String sql = "UPDATE seat "
                   + "SET seat_status = 'A' "
                   + "WHERE seat_id IN ( "
                   + "    SELECT seat_id "
                   + "    FROM booking_seat "
                   + "    WHERE booking_id = ? "
                   + ") AND seat_status = 'H'";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { bookingId });
        return jdbcUtil.executeUpdate();
    }

    // [수정] 단독용
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

    // [추가] 트랜잭션 참여용
    public int insertBookingSeat(JDBCUtil jdbcUtil, long bookingId, long seatId, double price) throws Exception {
        String sql = "INSERT INTO booking_seat(booking_seat_id, booking_id, seat_id, price_at_booking) "
                   + "VALUES (SEQ_BOOKING_SEAT.NEXTVAL, ?, ?, ?)";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { bookingId, seatId, price });
        return jdbcUtil.executeUpdate();
    }

    // [수정] 단독용
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

    // [추가] 트랜잭션 참여용. 0이 반환되면 이미 다른 사람이 잡은 좌석 → 호출한 쪽에서 롤백
    public int updateSeatToHeld(JDBCUtil jdbcUtil, long seatId) throws Exception {
        String sql = "UPDATE seat "
                   + "SET seat_status = 'H' "
                   + "WHERE seat_id = ? AND seat_status = 'A'";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatId });
        return jdbcUtil.executeUpdate();
    }

    private int executeChange(String sql, Object[] params) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();   // [추가]
        try {
            jdbcUtil.setSqlAndParameters(sql, params);

            int result = jdbcUtil.executeUpdate();

            jdbcUtil.commit();

            return result;
        }
        catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   // [수정]

            throw toSQLException("예매-좌석 처리 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    }

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
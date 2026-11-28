package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;   
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import model.domain.enums.BookingStatus;
import model.domain.enums.SeatStatus;
import org.springframework.stereotype.Repository;
import model.dto.BookingResponse;
import model.dto.BookingResponseDTO;
import model.dto.BookingSeatDTO;

@Repository
public class BookingDAO {
    /** [신규] 홀드 유지 시간(분). 만료 판단은 모두 DB 시간(SYSDATE) 기준 */
    public static final int HOLD_MINUTES = 10;
    
    /**
     * M22 - 예매 생성
     * 결제 전 상태(W)로 예매 정보를 생성하고 BOOKING_ID 반환
     */

    /** 단독용: 한 번에 commit */   
    public Long insertBooking(Long memberId, Long performanceId) throws SQLException { 
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            Long bookingId = insertBooking(jdbcUtil, memberId, performanceId);  
            jdbcUtil.commit();
            return bookingId;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   
            throw toSQLException("예매 생성 실패", ex);  
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * 트랜잭션 참여용: commit/rollback/close는 호출한 쪽(Manager)이 한다.
     * 결제 전 상태(W)로 예매를 만들고 BOOKING_ID 반환
     */   
    public Long insertBooking(JDBCUtil jdbcUtil, Long memberId, Long performanceId) throws Exception {
            String sql =
                "INSERT INTO BOOKING " +
                "(BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID) " +
                "VALUES (SEQ_BOOKING.NEXTVAL, ?, SYSDATE, ?, NULL, ?)";

            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                BookingStatus.WAITING_PAYMENT.getCode(), memberId, performanceId
            });

            jdbcUtil.executeUpdate();

            // 같은 DB 연결에서 방금 생성한 시퀀스 값 조회
            jdbcUtil.setSqlAndParameters(
                "SELECT SEQ_BOOKING.CURRVAL FROM DUAL",
                new Object[] {}
            );

            ResultSet rs = jdbcUtil.executeQuery();

            if (!rs.next()) {
                throw new SQLException("BOOKING_ID를 가져오지 못했습니다.");
            }
            return rs.getLong(1);
    }

    /** 선점용 예매 생성: W 상태 + 만료 시각(DB 시간 기준) 함께 저장. BOOKING_ID 반환 */
    public Long insertHoldBooking(JDBCUtil jdbcUtil, Long memberId, Long performanceId) throws Exception {
        String sql =
            "INSERT INTO BOOKING " +
            "(BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID, HOLD_EXPIRES_AT) " +
            "VALUES (SEQ_BOOKING.NEXTVAL, ?, SYSDATE, ?, NULL, ?, SYSDATE + ?/1440)";

        jdbcUtil.setSqlAndParameters(sql, new Object[] {
            BookingStatus.WAITING_PAYMENT.getCode(), memberId, performanceId, HOLD_MINUTES
        });
        jdbcUtil.executeUpdate();

        jdbcUtil.setSqlAndParameters("SELECT SEQ_BOOKING.CURRVAL FROM DUAL", new Object[] {});
        ResultSet rs = jdbcUtil.executeQuery();
        if (!rs.next()) {
            throw new SQLException("BOOKING_ID를 가져오지 못했습니다.");
        }
        return rs.getLong(1);
    }

    /** 공연의 1인 최대 예매 매수. 공연이 없으면 -1, NULL이면 제한 없음 */
    public int selectMaxTicketsPerMember(JDBCUtil jdbcUtil, Long performanceId) throws Exception {
        jdbcUtil.setSqlAndParameters(
            "SELECT MAX_TICKETS_PER_MEMBER FROM PERFORMANCE WHERE PERFORMANCE_ID = ?",
            new Object[] { performanceId });
        ResultSet rs = jdbcUtil.executeQuery();
        if (!rs.next()) return -1;
        int max = rs.getInt(1);
        return rs.wasNull() ? Integer.MAX_VALUE : max;
    }

    /** 이 회원이 이 공연에서 이미 확정(C)한 좌석 수 */
    public int countConfirmedSeats(JDBCUtil jdbcUtil, Long memberId, Long performanceId) throws Exception {
        jdbcUtil.setSqlAndParameters(
            "SELECT COUNT(*) FROM BOOKING_SEAT BS " +
            "JOIN BOOKING B ON B.BOOKING_ID = BS.BOOKING_ID " +
            "WHERE B.MEMBER_ID = ? AND B.PERFORMANCE_ID = ? AND B.BOOKING_STATUS = ?",
            new Object[] { memberId, performanceId, BookingStatus.CONFIRMED.getCode() });
        ResultSet rs = jdbcUtil.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    /** 이 회원의 이 공연 결제대기(W) 예매 ID 목록 (재선택 시 기존 홀드 교체용) */
    public List<Long> selectWaitingBookingIds(JDBCUtil jdbcUtil, Long memberId, Long performanceId) throws Exception {
        jdbcUtil.setSqlAndParameters(
            "SELECT BOOKING_ID FROM BOOKING " +
            "WHERE MEMBER_ID = ? AND PERFORMANCE_ID = ? AND BOOKING_STATUS = ?",
            new Object[] { memberId, performanceId, BookingStatus.WAITING_PAYMENT.getCode() });
        ResultSet rs = jdbcUtil.executeQuery();
        List<Long> ids = new ArrayList<>();
        while (rs.next()) ids.add(rs.getLong(1));
        return ids;
    }

    /** 만료 시간이 지난 결제대기(W) 예매 ID 목록 */
    public List<Long> selectExpiredWaitingIds(JDBCUtil jdbcUtil) throws Exception {
        jdbcUtil.setSqlAndParameters(
            "SELECT BOOKING_ID FROM BOOKING " +
            "WHERE BOOKING_STATUS = ? AND HOLD_EXPIRES_AT < SYSDATE",
            new Object[] { BookingStatus.WAITING_PAYMENT.getCode() });
        ResultSet rs = jdbcUtil.executeQuery();
        List<Long> ids = new ArrayList<>();
        while (rs.next()) ids.add(rs.getLong(1));
        return ids;
    }

    /**
     * W → X (조건부). 1이면 내가 처리, 0이면 그 사이 결제확정/취소 등으로 상태가 바뀜.
     * 사용자 재선택, 스케줄러 만료 양쪽에서 공용으로 사용.
     */
    public int cancelWaitingBooking(JDBCUtil jdbcUtil, Long bookingId) throws Exception {
        jdbcUtil.setSqlAndParameters(
            "UPDATE BOOKING SET BOOKING_STATUS = ? WHERE BOOKING_ID = ? AND BOOKING_STATUS = ?",
            new Object[] { BookingStatus.CANCELED.getCode(), bookingId,
                           BookingStatus.WAITING_PAYMENT.getCode() });
        return jdbcUtil.executeUpdate();
    }

    /** 홀드 남은 시간(초). DB 시간 기준 */
    public int selectRemainingSeconds(JDBCUtil jdbcUtil, Long bookingId) throws Exception {
        jdbcUtil.setSqlAndParameters(
            "SELECT ROUND((HOLD_EXPIRES_AT - SYSDATE) * 86400) FROM BOOKING WHERE BOOKING_ID = ?",
            new Object[] { bookingId });
        ResultSet rs = jdbcUtil.executeQuery();
        rs.next();
        return Math.max(0, rs.getInt(1));
    }
    
    /**
     * M16 - 회원의 예매 내역 조회
     */
    public List<BookingResponseDTO> selectBookingsByMemberId(Long memberId)
            throws Exception {  

        JDBCUtil jdbcUtil = new JDBCUtil();
        List<BookingResponseDTO> bookingList = new ArrayList<>();

        try {
            String sql =
                "SELECT B.BOOKING_ID, " +
                "       B.PERFORMANCE_ID, " +
                "       P.PERFORMANCE_NAME, " +
                "       B.BOOKING_STATUS, " +
                "       B.CREATED_AT " +
                "FROM BOOKING B " +
                "JOIN PERFORMANCE P " +
                "  ON B.PERFORMANCE_ID = P.PERFORMANCE_ID " +
                "WHERE B.MEMBER_ID = ? " +
                "ORDER BY B.CREATED_AT DESC";

            jdbcUtil.setSqlAndParameters(sql, new Object[] { memberId });

            ResultSet rs = jdbcUtil.executeQuery();

            while (rs.next()) {
                BookingResponseDTO booking = new BookingResponseDTO(
                    rs.getLong("BOOKING_ID"),
                    rs.getLong("PERFORMANCE_ID"),
                    rs.getString("PERFORMANCE_NAME"),
                    BookingStatus.fromCode(rs.getString("BOOKING_STATUS")),
                    rs.getTimestamp("CREATED_AT")
                );

                bookingList.add(booking);
            }

            return bookingList;
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M16 - 예매 상세 조회
     * 예매 정보와 예매 좌석 목록을 함께 조회
     */
    public BookingResponse selectBookingDetail(Long bookingId, Long memberId)
            throws Exception {   

        JDBCUtil jdbcUtil = new JDBCUtil();
        BookingResponse booking = null;

        try {
            String sql =
                "SELECT B.BOOKING_ID, " +
                "       B.MEMBER_ID, " +
                "       B.PERFORMANCE_ID, " +
                "       P.PERFORMANCE_NAME, " +
                "       B.BOOKING_STATUS, " +
                "       B.CREATED_AT, " +
                "       B.REFUND_ACCOUNT, " +
                "       S.SEAT_ID, " +
                "       S.SEAT_NUMBER, " +
                "       S.ROW_NUM, " +
                "       S.COLUMN_NUM, " +
                "       SC.SECTOR_NAME, " +
                "       SG.GRADE_NAME, " +
                "       BS.PRICE_AT_BOOKING " +
                "FROM BOOKING B " +
                "JOIN PERFORMANCE P " +
                "  ON B.PERFORMANCE_ID = P.PERFORMANCE_ID " +
                "LEFT JOIN BOOKING_SEAT BS " +
                "  ON B.BOOKING_ID = BS.BOOKING_ID " +
                "LEFT JOIN SEAT S " +
                "  ON BS.SEAT_ID = S.SEAT_ID " +
                "LEFT JOIN SECTOR SC " +
                "  ON S.SECTOR_ID = SC.SECTOR_ID " +
                "LEFT JOIN SEAT_GRADE SG " +
                "  ON SC.GRADE_ID = SG.GRADE_ID " +
                " AND SC.PERFORMANCE_ID = SG.PERFORMANCE_ID " +
                "WHERE B.BOOKING_ID = ? " +
                "  AND B.MEMBER_ID = ? " +
                "ORDER BY S.SEAT_ID";

            jdbcUtil.setSqlAndParameters(
                sql,
                new Object[] { bookingId, memberId }
            );

            ResultSet rs = jdbcUtil.executeQuery();

            while (rs.next()) {

                // 첫 번째 행에서 예매 기본 정보 생성
                if (booking == null) {
                    booking = new BookingResponse();

                    booking.setBookingId(rs.getLong("BOOKING_ID"));
                    booking.setMemberId(rs.getLong("MEMBER_ID"));
                    booking.setPerformanceId(rs.getLong("PERFORMANCE_ID"));
                    booking.setPerformanceName(rs.getString("PERFORMANCE_NAME"));
                    booking.setBookingStatus(BookingStatus.fromCode(rs.getString("BOOKING_STATUS")));
                    booking.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                    booking.setRefundAccount(rs.getString("REFUND_ACCOUNT"));
                }

                // 예매 좌석 정보 추가
                Long seatId = rs.getLong("SEAT_ID");

                if (!rs.wasNull()) {
                    BookingSeatDTO seat = new BookingSeatDTO(
                        seatId,
                        rs.getString("SEAT_NUMBER"),
                        rs.getString("ROW_NUM"),
                        rs.getLong("COLUMN_NUM"),
                        rs.getString("SECTOR_NAME"),
                        rs.getString("GRADE_NAME"),
                        rs.getBigDecimal("PRICE_AT_BOOKING")
                    );

                    booking.addSeat(seat);
                }
            }

            return booking;
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M23 - 예매 확정
     * 결제가 완료된 예매의 상태를 W → C로 변경
     */

     /** 단독용 */   
    public int updateBookingStatusToConfirmed(Long bookingId, Long memberId)
            throws SQLException {  
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateBookingStatusToConfirmed(jdbcUtil, bookingId, memberId);   // [수정]
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil); 
            throw toSQLException("예매 확정 실패", ex);  
        } finally {
            jdbcUtil.close();
        }
    }

   /** 트랜잭션 참여용. 0이 반환되면 이미 처리됐거나 W 상태가 아님 → 호출한 쪽에서 롤백 */   
    public int updateBookingStatusToConfirmed(JDBCUtil jdbcUtil, Long bookingId, Long memberId) throws Exception {
            String sql =
                "UPDATE BOOKING " +
                "SET BOOKING_STATUS = ? " +
                "WHERE BOOKING_ID = ? " +
                "  AND MEMBER_ID = ? " +
                "  AND BOOKING_STATUS = ? " +
                "  AND HOLD_EXPIRES_AT > SYSDATE";

            jdbcUtil.setSqlAndParameters(
                sql,
                new Object[] { BookingStatus.CONFIRMED.getCode(), bookingId, memberId, BookingStatus.WAITING_PAYMENT.getCode() }
            );

            return jdbcUtil.executeUpdate();
    }

    /**
     * M17 - 예매 취소
     * 확정된 예매의 상태를 C → X로 변경하고 환불 계좌 저장
     */

    /** 단독용 */  
    public int updateBookingStatusToCancelled(Long bookingId, Long memberId, String refundAccount)
            throws SQLException {  
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateBookingStatusToCancelled(jdbcUtil, bookingId, memberId, refundAccount);   // [수정]
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);  
            throw toSQLException("예매 취소 실패", ex);  
        } finally {
            jdbcUtil.close();
        }
    }

    /** 트랜잭션 참여용. 0이면 C 상태가 아님 → 호출한 쪽에서 롤백 */  
    public int updateBookingStatusToCancelled(JDBCUtil jdbcUtil, Long bookingId, Long memberId, String refundAccount) throws Exception {
            String sql =
                "UPDATE BOOKING " +
                "SET BOOKING_STATUS = ?, " +
                "    REFUND_ACCOUNT = ? " +
                "WHERE BOOKING_ID = ? " +
                "  AND MEMBER_ID = ? " +
                "  AND BOOKING_STATUS = ?";

            jdbcUtil.setSqlAndParameters(
                sql,
                new Object[] {
                    BookingStatus.CANCELED.getCode(), refundAccount, bookingId, memberId, BookingStatus.CONFIRMED.getCode() 
                }
            );

            return jdbcUtil.executeUpdate();
    }

    /**
     * M17 - 취소 대상 좌석 상태 변경
     * 예매 취소된 좌석의 상태를 B → C로 변경
     */

    /** 단독용 */
    public int updateSeatStatusToCancelWaiting(Long bookingId, Long memberId)
            throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateSeatStatusToCancelWaiting(jdbcUtil, bookingId, memberId);   // [수정]
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);  
            throw toSQLException("취소 좌석 상태 변경 실패", ex);  
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * 트랜잭션 참여용.
     * 주의: 서브쿼리가 BOOKING_STATUS = 'X'를 요구하므로
     *       같은 트랜잭션에서 updateBookingStatusToCancelled를 먼저 호출해야 한다.
     */   
    public int updateSeatStatusToCancelWaiting(JDBCUtil jdbcUtil, Long bookingId, Long memberId) throws Exception  {
            String sql =
                "UPDATE SEAT " +
                "SET SEAT_STATUS = ? " +
                "WHERE SEAT_ID IN ( " +
                "    SELECT BS.SEAT_ID " +
                "    FROM BOOKING_SEAT BS " +
                "    JOIN BOOKING B " +
                "      ON BS.BOOKING_ID = B.BOOKING_ID " +
                "    WHERE B.BOOKING_ID = ? " +
                "      AND B.MEMBER_ID = ? " +
                "      AND B.BOOKING_STATUS = ? " +
                ") " +
                "AND SEAT_STATUS = ?";

            jdbcUtil.setSqlAndParameters(
                sql,
                new Object[] {
                    SeatStatus.CANCEL_PENDING.getCode(), bookingId, memberId, BookingStatus.CANCELED.getCode(), SeatStatus.BOOKED.getCode() 
                }
            );

            return jdbcUtil.executeUpdate();
    }

    /**
     * M22 - 홀드 만료 처리
     * 생성 후 10분이 지난 결제 대기 예매를 만료 처리
     * [Deprecated] 기존 일괄 만료 처리: 스케줄러에서는 쓰지 않음
     * 좌석/예매를 따로 UPDATE해서, 그 사이 결제 확정되면 확정 좌석이 풀릴 수 있음.
     * 스케줄러는 BookingManager.expireHolds() (예매 단위 조건부 UPDATE) 사용.
     */
    @Deprecated
    public int expireBookings() throws SQLException {   
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            // 1. 만료 대상 예매의 좌석 상태 H → A 변경
            String seatSql =
                "UPDATE SEAT S " +
                "SET SEAT_STATUS = ? " +
                "WHERE S.SEAT_STATUS = ? " +
                "AND EXISTS ( " +
                "    SELECT 1 " +
                "    FROM BOOKING_SEAT BS " +
                "    JOIN BOOKING B ON BS.BOOKING_ID = B.BOOKING_ID " +
                "    WHERE BS.SEAT_ID = S.SEAT_ID " +
                "    AND B.BOOKING_STATUS = ? " +
                "    AND B.HOLD_EXPIRES_AT < SYSDATE " +
                ")";

            jdbcUtil.setSqlAndParameters(
                seatSql,
                new Object[] { SeatStatus.AVAILABLE.getCode(), SeatStatus.HELD.getCode(),
                        BookingStatus.WAITING_PAYMENT.getCode() }
            );
            jdbcUtil.executeUpdate();

            // 2. 만료 대상 예매 상태 변경
            String bookingSql =
                "UPDATE BOOKING " +
                "SET BOOKING_STATUS = ? " +
                "WHERE BOOKING_STATUS = ? " +
                "AND HOLD_EXPIRES_AT < SYSDATE";

            jdbcUtil.setSqlAndParameters(
                bookingSql,
                new Object[] { BookingStatus.CANCELED.getCode(), BookingStatus.WAITING_PAYMENT.getCode() }
            );

            int result = jdbcUtil.executeUpdate();

            // 3. 마지막에 한 번만 commit
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);  
            throw toSQLException("홀드 만료 처리 실패", ex);   
        } finally {
            jdbcUtil.close();
        }
    }
  
    // =====================================================================
    // 내부 공통 처리
    // =====================================================================

    /** 연결을 얻기 전에 실패한 경우에도 원래 예외가 가려지지 않게 한다 */  
    private void rollbackQuietly(JDBCUtil jdbcUtil) {
        try {
            jdbcUtil.rollback();
        } catch (Exception ignore) {
            // 원래 예외를 그대로 전달하기 위해 무시
        }
    }

    private SQLException toSQLException(String message, Exception ex) {
        if (ex instanceof SQLException) {
            return (SQLException) ex;
        }
        return new SQLException(message, ex);
    }
}
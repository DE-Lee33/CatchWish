/*
 * [변경 요약] BookingDAO  (표시: [수정] [추가] [삭제])
 * [추가] import java.sql.SQLException
 * [수정] throws Exception -> throws SQLException (expireBookings 포함)
 * [추가] 트랜잭션 참여용 오버로드 4개: insertBooking, updateBookingStatusToConfirmed,
 *        updateBookingStatusToCancelled, updateSeatStatusToCancelWaiting
 * [수정] 위 4개의 단독용 메서드는 참여용을 호출하고 commit
 * [수정] expireBookings: rollback -> rollbackQuietly, throw ex -> toSQLException
 * [추가] private 헬퍼 rollbackQuietly, toSQLException
 */
package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;   // [추가]
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import model.dto.BookingResponse;
import model.dto.BookingResponseDTO;
import model.dto.BookingSeatDTO;

@Repository
public class BookingDAO {

    /**
     * M22 - 예매 생성
     * 결제 전 상태(W)로 예매 정보를 생성하고 BOOKING_ID 반환
     */

    /** 단독용: 한 번에 commit */   // [수정]
    public Long insertBooking(Long memberId, Long performanceId) throws SQLException {   // [수정]
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            Long bookingId = insertBooking(jdbcUtil, memberId, performanceId);   // [수정]
            jdbcUtil.commit();
            return bookingId;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   // [수정]
            throw toSQLException("예매 생성 실패", ex);   // [수정]
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * 트랜잭션 참여용: commit/rollback/close는 호출한 쪽(Manager)이 한다.
     * 결제 전 상태(W)로 예매를 만들고 BOOKING_ID 반환
     */   // [추가] 아래 메서드 전체
    public Long insertBooking(JDBCUtil jdbcUtil, Long memberId, Long performanceId) throws Exception {
            String sql =
                "INSERT INTO BOOKING " +
                "(BOOKING_ID, BOOKING_STATUS, CREATED_AT, MEMBER_ID, PAYMENT_ID, PERFORMANCE_ID) " +
                "VALUES (SEQ_BOOKING.NEXTVAL, ?, SYSDATE, ?, NULL, ?)";

            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                "W", memberId, performanceId
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

    /**
     * M16 - 회원의 예매 내역 조회
     */
    public List<BookingResponseDTO> selectBookingsByMemberId(Long memberId)
            throws Exception {   // [수정]

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
                    rs.getString("BOOKING_STATUS"),
                    rs.getTimestamp("CREATED_AT")
                );

                bookingList.add(booking);
            }

            return bookingList;
        // [삭제] catch (Exception ex) { throw ex; }  (아무 동작 없는 catch)
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M16 - 예매 상세 조회
     * 예매 정보와 예매 좌석 목록을 함께 조회
     */
    public BookingResponse selectBookingDetail(Long bookingId, Long memberId)
            throws Exception {   // [수정]

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
                    booking.setBookingStatus(rs.getString("BOOKING_STATUS"));
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
        // [삭제] catch (Exception ex) { throw ex; }  (아무 동작 없는 catch)
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M23 - 예매 확정
     * 결제가 완료된 예매의 상태를 W → C로 변경
     */

     /** 단독용 */   // [수정]
    public int updateBookingStatusToConfirmed(Long bookingId, Long memberId)
            throws SQLException {   // [수정]
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateBookingStatusToConfirmed(jdbcUtil, bookingId, memberId);   // [수정]
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   // [수정]
            throw toSQLException("예매 확정 실패", ex);   // [수정]
        } finally {
            jdbcUtil.close();
        }
    }

   /** 트랜잭션 참여용. 0이 반환되면 이미 처리됐거나 W 상태가 아님 → 호출한 쪽에서 롤백 */   // [추가] 아래 메서드 전체
    public int updateBookingStatusToConfirmed(JDBCUtil jdbcUtil, Long bookingId, Long memberId) throws Exception {
            String sql =
                "UPDATE BOOKING " +
                "SET BOOKING_STATUS = ? " +
                "WHERE BOOKING_ID = ? " +
                "  AND MEMBER_ID = ? " +
                "  AND BOOKING_STATUS = ?";

            jdbcUtil.setSqlAndParameters(
                sql,
                new Object[] { "C", bookingId, memberId, "W" }
            );

            return jdbcUtil.executeUpdate();
    }

    /**
     * M17 - 예매 취소
     * 확정된 예매의 상태를 C → X로 변경하고 환불 계좌 저장
     */

    /** 단독용 */   // [수정]
    public int updateBookingStatusToCancelled(Long bookingId, Long memberId, String refundAccount)
            throws SQLException {   // [수정]
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateBookingStatusToCancelled(jdbcUtil, bookingId, memberId, refundAccount);   // [수정]
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   // [수정]
            throw toSQLException("예매 취소 실패", ex);   // [수정]
        } finally {
            jdbcUtil.close();
        }
    }

 /** 트랜잭션 참여용. 0이면 C 상태가 아님 → 호출한 쪽에서 롤백 */   // [추가] 아래 메서드 전체
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
                    "X", refundAccount, bookingId, memberId, "C"
                }
            );

            return jdbcUtil.executeUpdate();
    }

    /**
     * M17 - 취소 대상 좌석 상태 변경
     * 예매 취소된 좌석의 상태를 B → C로 변경
     */

   /** 단독용 */   // [수정]
    public int updateSeatStatusToCancelWaiting(Long bookingId, Long memberId)
            throws SQLException {   // [수정]
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateSeatStatusToCancelWaiting(jdbcUtil, bookingId, memberId);   // [수정]
            jdbcUtil.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   // [수정]
            throw toSQLException("취소 좌석 상태 변경 실패", ex);   // [수정]
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * 트랜잭션 참여용.
     * 주의: 서브쿼리가 BOOKING_STATUS = 'X'를 요구하므로
     *       같은 트랜잭션에서 updateBookingStatusToCancelled를 먼저 호출해야 한다.
     */   // [추가] 아래 메서드 전체
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
                    "C", bookingId, memberId, "X", "B"
                }
            );

            return jdbcUtil.executeUpdate();
    }

    /**
     * M22 - 홀드 만료 처리
     * 생성 후 10분이 지난 결제 대기 예매를 만료 처리
     */
    public int expireBookings() throws SQLException {   // [수정] throws Exception -> SQLException
        JDBCUtil jdbcUtil = new JDBCUtil();

        // 기준 시간: 현재 시각에서 10분 전
        Timestamp cutoffTime = new Timestamp(
            System.currentTimeMillis() - 10 * 60 * 1000L
        );

        try {
            // 1. 만료 대상 예매의 좌석 상태 H → A 변경
            String seatSql =
                "UPDATE SEAT S " +
                "SET SEAT_STATUS = 'A' " +
                "WHERE S.SEAT_STATUS = 'H' " +
                "AND EXISTS ( " +
                "    SELECT 1 " +
                "    FROM BOOKING_SEAT BS " +
                "    JOIN BOOKING B ON BS.BOOKING_ID = B.BOOKING_ID " +
                "    WHERE BS.SEAT_ID = S.SEAT_ID " +
                "    AND B.BOOKING_STATUS = 'W' " +
                "    AND B.CREATED_AT < ? " +
                ")";

            jdbcUtil.setSqlAndParameters(
                seatSql,
                new Object[] { cutoffTime }
            );
            jdbcUtil.executeUpdate();

            // 2. 만료 대상 예매 상태 변경
            String bookingSql =
                "UPDATE BOOKING " +
                "SET BOOKING_STATUS = 'X' " +
                "WHERE BOOKING_STATUS = 'W' " +
                "AND CREATED_AT < ?";

            jdbcUtil.setSqlAndParameters(
                bookingSql,
                new Object[] { cutoffTime }
            );

            int result = jdbcUtil.executeUpdate();

            // 3. 마지막에 한 번만 commit
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   // [수정] jdbcUtil.rollback() -> rollbackQuietly(jdbcUtil)
            throw toSQLException("홀드 만료 처리 실패", ex);   // [수정] throw ex -> toSQLException

        } finally {
            jdbcUtil.close();
        }
    }

// =====================================================================
    // 내부 공통 처리
    // =====================================================================

    /** 연결을 얻기 전에 실패한 경우에도 원래 예외가 가려지지 않게 한다 */   // [추가] 헬퍼 2개
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

package model.dao;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import model.dto.BookingResponse;
import model.dto.BookingResponseDTO;
import model.dto.BookingSeatDTO;

public class BookingDAO {

    /**
     * M22 - 예매 생성
     * 결제 전 상태(W)로 예매 정보를 생성하고 BOOKING_ID 반환
     */
    public Long insertBooking(Long memberId, Long performanceId) throws Exception {
        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
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

            Long bookingId = null;

            if (rs.next()) {
                bookingId = rs.getLong(1);
            }

            jdbcUtil.commit();
            return bookingId;

        } catch (Exception ex) {
            jdbcUtil.rollback();
            throw ex;
        } finally {
            jdbcUtil.close();
        }
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
                    rs.getString("BOOKING_STATUS"),
                    rs.getTimestamp("CREATED_AT")
                );

                bookingList.add(booking);
            }

            return bookingList;

        } catch (Exception ex) {
            throw ex;
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

        } catch (Exception ex) {
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M23 - 예매 확정
     * 결제가 완료된 예매의 상태를 W → C로 변경
     */
    public int updateBookingStatusToConfirmed(
            Long bookingId, Long memberId) throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
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

            int result = jdbcUtil.executeUpdate();

            jdbcUtil.commit();
            return result;

        } catch (Exception ex) {
            jdbcUtil.rollback();
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M17 - 예매 취소
     * 확정된 예매의 상태를 C → X로 변경하고 환불 계좌 저장
     */
    public int updateBookingStatusToCancelled(
            Long bookingId, Long memberId, String refundAccount)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
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

            int result = jdbcUtil.executeUpdate();

            jdbcUtil.commit();
            return result;

        } catch (Exception ex) {
            jdbcUtil.rollback();
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M17 - 취소 대상 좌석 상태 변경
     * 예매 취소된 좌석의 상태를 B → C로 변경
     */
    public int updateSeatStatusToCancelWaiting(
            Long bookingId, Long memberId) throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
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

            int result = jdbcUtil.executeUpdate();

            jdbcUtil.commit();
            return result;

        } catch (Exception ex) {
            jdbcUtil.rollback();
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    /**
     * M22 - 홀드 만료 처리
     * 생성 후 10분이 지난 결제 대기 예매를 만료 처리
     */
    public int expireBookings() throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            String sql =
                "UPDATE BOOKING " +
                "SET BOOKING_STATUS = ? " +
                "WHERE BOOKING_STATUS = ? " +
                "  AND CREATED_AT <= SYSDATE - (10 / 1440)";

            jdbcUtil.setSqlAndParameters(
                sql,
                new Object[] { "X", "W" }
            );

            int result = jdbcUtil.executeUpdate();

            jdbcUtil.commit();
            return result;

        } catch (Exception ex) {
            jdbcUtil.rollback();
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }
}

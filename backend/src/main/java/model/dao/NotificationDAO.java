package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.domain.Notification;
import model.domain.Wishlist;

public class NotificationDAO {
    
    // 알림 생성
    public int insertNotification(Notification notification) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "INSERT INTO NOTIFICATION "
                + "(NOTIFICATION_ID, NOTIFICATION_TYPE, SEND_STATUS, "
                + "CREATED_AT, MEMBER_ID, IS_READ, WISHLIST_ID, "
                + "CANCELED_TICKET_SCHEDULE_ID) "
                + "VALUES (SEQ_NOTIFICATION.NEXTVAL, ?, 'N', SYSDATE, ?, 'N', ?, ?)";

        Object[] params = {
                notification.getNotificationType(),
                notification.getMemberId(),
                notification.getWishlistId(),
                notification.getCanceledTicketScheduleId()
        };

        try {
            jdbcUtil.setSqlAndParameters(sql, params);
            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();
            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("알림 생성 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 취소표 일정에 해당하는 위시리스트 알림 대상 조회
    public List<Wishlist> selectWishlistTargets(
            Long scheduleId, Long sectorId) {

        JDBCUtil jdbcUtil = new JDBCUtil();
        List<Wishlist> wishlists = new ArrayList<>();

        String sql = "SELECT w.WISHLIST_ID, w.MEMBER_ID, "
                + "w.PERFORMANCE_ID, w.SECTOR_ID "
                + "FROM WISHLIST w "
                + "JOIN CANCELED_TICKET_SCHEDULE c "
                + "ON w.PERFORMANCE_ID = c.PERFORMANCE_ID "
                + "WHERE c.CANCELED_TICKET_SCHEDULE_ID = ? "
                + "AND (w.SECTOR_ID = ? OR w.SECTOR_ID IS NULL)";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{scheduleId, sectorId});

            ResultSet rs = jdbcUtil.executeQuery();
            if (rs == null) {
                throw new SQLException("알림 대상 조회 실패");
            }

            while (rs.next()) {
                Wishlist wishlist = new Wishlist();
                wishlist.setWishlistId(rs.getLong("WISHLIST_ID"));
                wishlist.setMemberId(rs.getLong("MEMBER_ID"));
                wishlist.setPerformanceId(rs.getLong("PERFORMANCE_ID"));

                long sector = rs.getLong("SECTOR_ID");
                wishlist.setSectorId(rs.wasNull() ? null : sector);

                wishlists.add(wishlist);
            }

            return wishlists;

        } catch (Exception e) {
            throw new RuntimeException("위시리스트 알림 대상 조회 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 발송 대기 중인 알림 조회
    public List<Notification> selectPendingNotifications() {
        JDBCUtil jdbcUtil = new JDBCUtil();
        List<Notification> notifications = new ArrayList<>();

        String sql = "SELECT * FROM NOTIFICATION "
                + "WHERE SEND_STATUS = 'N' "
                + "ORDER BY CREATED_AT";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[]{});

            ResultSet rs = jdbcUtil.executeQuery();
            if (rs == null) {
                throw new SQLException("발송 대기 알림 조회 실패");
            }

            while (rs.next()) {
                notifications.add(mapNotification(rs));
            }

            return notifications;

        } catch (Exception e) {
            throw new RuntimeException("발송 대기 알림 조회 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 알림 발송 상태 변경
    public int updateSendStatus(
            Long notificationId, String sendStatus) {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "UPDATE NOTIFICATION "
                + "SET SEND_STATUS = ?, "
                + "SEND_AT = CASE WHEN ? = 'Y' "
                + "THEN SYSDATE ELSE SEND_AT END "
                + "WHERE NOTIFICATION_ID = ? "
                + "AND SEND_STATUS = 'N'";

        Object[] params = {
                sendStatus, sendStatus, notificationId
        };

        try {
            jdbcUtil.setSqlAndParameters(sql, params);
            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();
            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("알림 발송 상태 변경 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 회원별 알림 목록 조회
    public List<Notification> selectNotificationByMemberId(Long memberId) {
        JDBCUtil jdbcUtil = new JDBCUtil();
        List<Notification> notifications = new ArrayList<>();

        String sql = "SELECT * FROM NOTIFICATION "
                + "WHERE MEMBER_ID = ? "
                + "ORDER BY CREATED_AT DESC";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{memberId});

            ResultSet rs = jdbcUtil.executeQuery();
            if (rs == null) {
                throw new SQLException("회원별 알림 조회 실패");
            }

            while (rs.next()) {
                notifications.add(mapNotification(rs));
            }

            return notifications;

        } catch (Exception e) {
            throw new RuntimeException("회원별 알림 조회 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 회원이 알림을 읽음 처리
    public int updateReadStatus(Long notificationId, Long memberId) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "UPDATE NOTIFICATION "
                + "SET IS_READ = 'Y' "
                + "WHERE NOTIFICATION_ID = ? "
                + "AND MEMBER_ID = ? "
                + "AND IS_READ = 'N'";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{notificationId, memberId});

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();
            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("알림 읽음 처리 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 조회 결과를 Notification 객체로 변환
    private Notification mapNotification(ResultSet rs)
            throws SQLException {

        Notification notification = new Notification();

        notification.setNotificationId(rs.getLong("NOTIFICATION_ID"));
        notification.setNotificationType(rs.getString("NOTIFICATION_TYPE"));
        notification.setSendStatus(rs.getString("SEND_STATUS"));
        notification.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        notification.setSendAt(rs.getTimestamp("SEND_AT"));
        notification.setMemberId(rs.getLong("MEMBER_ID"));
        notification.setIsRead(rs.getString("IS_READ"));

        long wishlistId = rs.getLong("WISHLIST_ID");
        notification.setWishlistId(rs.wasNull() ? null : wishlistId);

        long scheduleId = rs.getLong("CANCELED_TICKET_SCHEDULE_ID");
        notification.setCanceledTicketScheduleId(
                rs.wasNull() ? null : scheduleId);

        return notification;
    }
}

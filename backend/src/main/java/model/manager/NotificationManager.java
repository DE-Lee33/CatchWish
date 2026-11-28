package model.manager;

import java.util.List;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import model.dao.JDBCUtil;
import model.domain.Wishlist;

import org.springframework.stereotype.Service;

import model.dao.NotificationDAO;
import model.domain.Notification;

@Service
public class NotificationManager {
    private final NotificationDAO notificationDAO;

    public NotificationManager(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    // 로그인한 회원의 알림 목록을 최신순으로 조회(M32-1)
    public List<Notification> getNotifications(long memberId) {
        return notificationDAO.selectNotificationByMemberId(memberId);
    }
    
    // 취소 대기 구역에 해당하는 위시리스트에 재오픈 알림을 생성(M28)
    public void createCancelTicketNotifications(
            JDBCUtil jdbcUtil, long performanceId, long scheduleId) throws Exception {
        List<Long> sectorIds = notificationDAO.selectCancelPendingSectorIds(performanceId);
        Set<Long> notifiedWishlistIds = new HashSet<>();

        for (Long sectorId : sectorIds) {
            List<Wishlist> targets =
                    notificationDAO.selectWishlistTargets(jdbcUtil, scheduleId, sectorId);

            for (Wishlist wishlist : targets) {
                // 공연만 찜한 위시리스트는 여러 구역에서 조회될 수 있어 알림을 한 번만 만든다.
                if (!notifiedWishlistIds.add(wishlist.getWishlistId())) {
                    continue;
                }

                Notification notification = new Notification();
                notification.setNotificationType("CANCEL");
                notification.setMemberId(wishlist.getMemberId());
                notification.setWishlistId(wishlist.getWishlistId());
                notification.setCanceledTicketScheduleId(scheduleId);

                if (notificationDAO.insertNotification(jdbcUtil, notification) != 1) {
                    throw new SQLException("취소표 알림을 저장하지 못했습니다.");
                }
            }
        }
    }
}
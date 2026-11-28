package model.manager;

import java.util.List;

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
}
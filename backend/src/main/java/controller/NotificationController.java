package controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import common.auth.LoginRequired;
import common.auth.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import model.manager.NotificationManager;
import model.domain.Notification;

@RestController
public class NotificationController {
    private final NotificationManager notificationManager;

    public NotificationController(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    // 로그인한 회원의 알림 목록을 최신순으로 조회한다.(M32-1)
    @LoginRequired
    @GetMapping("/api/notifications")
    public List<Notification> getNotifications(HttpServletRequest request) {
        long memberId = LoginSession.require(request).memberId();
        return notificationManager.getNotifications(memberId);    
     }
}
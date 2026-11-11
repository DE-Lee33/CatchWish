package model.domain;

import java.sql.Timestamp;

public class Notification {
    private Long notificationId;
    private String notificationType;
    private String sendStatus;
    private Timestamp createdAt;
    private Timestamp sendAt;
    private Long memberId;
    private String isRead;
    private Long wishlistId;
    private Long canceledTicketScheduleId;
    
    public Notification() {}

    public Notification(Long notificationId, String notificationType,
                        String sendStatus, Timestamp createdAt,
                        Timestamp sendAt, Long memberId,
                        String isRead, Long wishlistId,
                        Long canceledTicketScheduleId) {
        this.notificationId = notificationId;
        this.notificationType = notificationType;
        this.sendStatus = sendStatus;
        this.createdAt = createdAt;
        this.sendAt = sendAt;
        this.memberId = memberId;
        this.isRead = isRead;
        this.wishlistId = wishlistId;
        this.canceledTicketScheduleId = canceledTicketScheduleId;
    }
    public Long getNotificationId() {
        return notificationId;
    }
    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
    public String getNotificationType() {
        return notificationType;
    }
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
    public String getSendStatus() {
        return sendStatus;
    }
    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public Timestamp getSendAt() {
        return sendAt;
    }
    public void setSendAt(Timestamp sendAt) {
        this.sendAt = sendAt;
    }
    public Long getMemberId() {
        return memberId;
    }
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    public String getIsRead() {
        return isRead;
    }
    public void setIsRead(String isRead) {
        this.isRead = isRead;
    }
    public Long getWishlistId() {
        return wishlistId;
    }
    public void setWishlistId(Long wishlistId) {
        this.wishlistId = wishlistId;
    }
    public Long getCanceledTicketScheduleId() {
        return canceledTicketScheduleId;
    }
    public void setCanceledTicketScheduleId(Long canceledTicketScheduleId) {
        this.canceledTicketScheduleId = canceledTicketScheduleId;
    }
}

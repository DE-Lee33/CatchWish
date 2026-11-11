
package model.domain;

import java.sql.Timestamp;

public class Queue {

    private Long queueId;
    private Integer position;
    private String status;
    private String authCode;
    private String isVerified;
    private Timestamp createdAt;
    private Long memberId;
    private Long performanceId;

    // 기본 생성자
    public Queue() {
    }

    // 대기열 기본 정보 생성자 (QueueDAO 조회용)
    public Queue(Long queueId, Integer position, String status) {
        this.queueId = queueId;
        this.position = position;
        this.status = status;
    }

    // 전체 생성자
    public Queue(Long queueId, Integer position, String status,
                 String authCode, String isVerified, Timestamp createdAt,
                 Long memberId, Long performanceId) {
        this.queueId = queueId;
        this.position = position;
        this.status = status;
        this.authCode = authCode;
        this.isVerified = isVerified;
        this.createdAt = createdAt;
        this.memberId = memberId;
        this.performanceId = performanceId;
    }

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(String isVerified) {
        this.isVerified = isVerified;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }
}

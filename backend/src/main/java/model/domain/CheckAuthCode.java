package model.domain;

import java.sql.Timestamp;

public class CheckAuthCode {
    private Long authCodeId;
    private Long memberId;
    private Long performanceId;
    private String isVerified;
    private Timestamp expiresAt;
    private Timestamp createdAt;
    
    public CheckAuthCode() {}

    public CheckAuthCode(Long authCodeId, Long memberId,
                         Long performanceId, String isVerified,
                         Timestamp expiresAt, Timestamp createdAt) {
        this.authCodeId = authCodeId;
        this.memberId = memberId;
        this.performanceId = performanceId;
        this.isVerified = isVerified;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }
    public Long getAuthCodeId() {
        return authCodeId;
    }
    public void setAuthCodeId(Long authCodeId) {
        this.authCodeId = authCodeId;
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
    public String getIsVerified() {
        return isVerified;
    }
    public void setIsVerified(String isVerified) {
        this.isVerified = isVerified;
    }
    public Timestamp getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}



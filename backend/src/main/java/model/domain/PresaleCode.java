package model.domain;

import java.sql.Timestamp;

public class PresaleCode {
    private Long codeId;
    private Long performanceId;
    private String codeValue;
    private int seatLimit;
    private Timestamp startAt;
    private Timestamp endAt;
    private Timestamp createdAt;
    
    public PresaleCode() {}

    public PresaleCode(Long codeId, Long performanceId,
                       String codeValue, int seatLimit,
                       Timestamp startAt, Timestamp endAt,
                       Timestamp createdAt) {
        this.codeId = codeId;
        this.performanceId = performanceId;
        this.codeValue = codeValue;
        this.seatLimit = seatLimit;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdAt = createdAt;
    }
    public Long getCodeId() {
        return codeId;
    }
    public void setCodeId(Long codeId) {
        this.codeId = codeId;
    }
    public Long getPerformanceId() {
        return performanceId;
    }
    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }
    public String getCodeValue() {
        return codeValue;
    }
    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }
    public int getSeatLimit() {
        return seatLimit;
    }
    public void setSeatLimit(int seatLimit) {
        this.seatLimit = seatLimit;
    }
    public Timestamp getStartAt() {
        return startAt;
    }
    public void setStartAt(Timestamp startAt) {
        this.startAt = startAt;
    }
    public Timestamp getEndAt() {
        return endAt;
    }
    public void setEndAt(Timestamp endAt) {
        this.endAt = endAt;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

package model.domain;

import java.sql.Timestamp;

import model.domain.enums.ScheduleStatus;

public class CanceledTicketSchedule {
    private Long canceledTicketScheduleId;
    private Timestamp openAt;
    private ScheduleStatus scheduleStatus;
    private Timestamp createdAt;
    private Long performanceId;
    
    public CanceledTicketSchedule() {}

    public CanceledTicketSchedule(Long canceledTicketScheduleId,
                                  Timestamp openAt,
                                  ScheduleStatus scheduleStatus,
                                  Timestamp createdAt,
                                  Long performanceId) {
        this.canceledTicketScheduleId = canceledTicketScheduleId;
        this.openAt = openAt;
        this.scheduleStatus = scheduleStatus;
        this.createdAt = createdAt;
        this.performanceId = performanceId;
    }
    public Long getCanceledTicketScheduleId() {
        return canceledTicketScheduleId;
    }
    public void setCanceledTicketScheduleId(Long canceledTicketScheduleId) {
        this.canceledTicketScheduleId = canceledTicketScheduleId;
    }
    public Timestamp getOpenAt() {
        return openAt;
    }
    public void setOpenAt(Timestamp openAt) {
        this.openAt = openAt;
    }
    public ScheduleStatus getScheduleStatus() {
        return scheduleStatus;
    }
    public void setScheduleStatus(ScheduleStatus scheduleStatus) {
        this.scheduleStatus = scheduleStatus;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public Long getPerformanceId() {
        return performanceId;
    }
    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }
}


package model.dto;

import model.domain.enums.QueueStatus;

public class QueueStatusDTO {

    private Long queueId;
    private Integer position;
    private QueueStatus status;
    private Integer waitingPosition;
    private Integer totalWaiting;

    // 기본 생성자
    public QueueStatusDTO() {
    }

    // 전체 생성자
    public QueueStatusDTO(Long queueId, Integer position, QueueStatus status,
                          Integer waitingPosition, Integer totalWaiting) {
        this.queueId = queueId;
        this.position = position;
        this.status = status;
        this.waitingPosition = waitingPosition;
        this.totalWaiting = totalWaiting;
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

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public Integer getWaitingPosition() {
        return waitingPosition;
    }

    public void setWaitingPosition(Integer waitingPosition) {
        this.waitingPosition = waitingPosition;
    }

    public Integer getTotalWaiting() {
        return totalWaiting;
    }

    public void setTotalWaiting(Integer totalWaiting) {
        this.totalWaiting = totalWaiting;
    }
}

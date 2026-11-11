
package model.dto;

public class QueueStatusDTO {

    private Long queueId;
    private Integer position;
    private String status;
    private Integer waitingPosition;
    private Integer totalWaiting;

    // 기본 생성자
    public QueueStatusDTO() {
    }

    // 전체 생성자
    public QueueStatusDTO(Long queueId, Integer position, String status,
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

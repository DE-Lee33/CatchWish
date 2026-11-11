package model.domain;

public class Sector {
    private long sectorId;
    private String sectorName; 
    private long seatGradeId;
    private long performanceId;
    private long seatLayoutId;

    public Sector() {
    }

    public Sector(long sectorId, String sectorName, long seatGradeId, long performanceId, long seatLayoutId) {
        super();
        this.sectorId = sectorId;
        this.sectorName = sectorName;
        this.seatGradeId = seatGradeId;
        this.performanceId = performanceId;
        this.seatLayoutId = seatLayoutId;
    }

    public long getSectorId() {
        return sectorId;
    }

    public void setSectorId(long sectorId) {
        this.sectorId = sectorId;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public long getSeatGradeId() {
        return seatGradeId;
    }

    public void setSeatGradeId(long seatGradeId) {
        this.seatGradeId = seatGradeId;
    }

    public long getPerformanceId() {
        return performanceId;
    }

    public void setPerformanceId(long performanceId) {
        this.performanceId = performanceId;
    }

    public long getSeatLayoutId() {
        return seatLayoutId;
    }

    public void setSeatLayoutId(long seatLayoutId) {
        this.seatLayoutId = seatLayoutId;
    }

    @Override
    public String toString() {
        return "Sector [sectorId=" + sectorId + ", sectorName=" + sectorName + ", seatGradeId=" + seatGradeId 
                + ", performanceId=" + performanceId + ", seatLayoutId=" + seatLayoutId + "]";
    }
}
package model.domain;

public class Seat {
    private long seatId;
    private long seatLayoutId;
    private String rowNum;      
    private long columnNum;      
    private String seatNumber;   
    private String seatStatus;  
    private long sectorId;

    public Seat() {
    }

    public Seat(long seatId, long seatLayoutId, String rowNum, long columnNum, String seatNumber, String seatStatus, long sectorId) {
        super();
        this.seatId = seatId;
        this.seatLayoutId = seatLayoutId;
        this.rowNum = rowNum;
        this.columnNum = columnNum;
        this.seatNumber = seatNumber;
        this.seatStatus = seatStatus;
        this.sectorId = sectorId;
    }

    public long getSeatId() {
        return seatId;
    }

    public void setSeatId(long seatId) {
        this.seatId = seatId;
    }

    public long getSeatLayoutId() {
        return seatLayoutId;
    }

    public void setSeatLayoutId(long seatLayoutId) {
        this.seatLayoutId = seatLayoutId;
    }

    public String getRowNum() {
        return rowNum;
    }

    public void setRowNum(String rowNum) {
        this.rowNum = rowNum;
    }

    public long getColumnNum() {
        return columnNum;
    }

    public void setColumnNum(long columnNum) {
        this.columnNum = columnNum;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatStatus() {
        return seatStatus;
    }

    public void setSeatStatus(String seatStatus) {
        this.seatStatus = seatStatus;
    }

    public long getSectorId() {
        return sectorId;
    }

    public void setSectorId(long sectorId) {
        this.sectorId = sectorId;
    }

    @Override
    public String toString() {
        return "Seat [seatId=" + seatId + ", seatLayoutId=" + seatLayoutId + ", rowNum=" + rowNum 
                + ", columnNum=" + columnNum + ", seatNumber=" + seatNumber + ", seatStatus=" + seatStatus 
                + ", sectorId=" + sectorId + "]";
    }
}

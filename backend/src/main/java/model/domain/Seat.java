package model.domain;

public class Seat {
    private long seatId;
    private long seatLayoutId;
    private long sectorId;
    private int rowNum;
    private int columnNum;
    private String seatNumber;
    private String seatStatus;
    
    public Seat() {}
    
    public Seat(long seatId, long seatLayoutId, long sectorId,
            int rowNum, int columnNum, String seatNumber, String seatStatus) {
        this.seatId = seatId;
        this.seatLayoutId = seatLayoutId;
        this.sectorId = sectorId;
        this.rowNum = rowNum;
        this.columnNum = columnNum;
        this.seatNumber = seatNumber;
        this.seatStatus = seatStatus;
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
    public long getSectorId() {
        return sectorId;
    }
    public void setSectorId(long sectorId) {
        this.sectorId = sectorId;
    }
    public int getRowNum() {
        return rowNum;
    }
    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }
    public int getColumnNum() {
        return columnNum;
    }
    public void setColumnNum(int columnNum) {
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
}

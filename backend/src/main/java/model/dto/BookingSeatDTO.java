
package model.dto;

import java.math.BigDecimal;

public class BookingSeatDTO {

    private Long seatId;
    private String seatNumber;
    private String rowNum;
    private Long columnNum;
    private String sectorName;
    private String gradeName;
    private BigDecimal priceAtBooking;

    public BookingSeatDTO() {
    }

    public BookingSeatDTO(Long seatId, String seatNumber,
                          String rowNum, Long columnNum,
                          String sectorName, String gradeName,
                          BigDecimal priceAtBooking) {
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.rowNum = rowNum;
        this.columnNum = columnNum;
        this.sectorName = sectorName;
        this.gradeName = gradeName;
        this.priceAtBooking = priceAtBooking;
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getRowNum() {
        return rowNum;
    }

    public Long getColumnNum() {
        return columnNum;
    }

    public String getSectorName() {
        return sectorName;
    }

    public String getGradeName() {
        return gradeName;
    }

    public BigDecimal getPriceAtBooking() {
        return priceAtBooking;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setRowNum(String rowNum) {
        this.rowNum = rowNum;
    }

    public void setColumnNum(Long columnNum) {
        this.columnNum = columnNum;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public void setPriceAtBooking(BigDecimal priceAtBooking) {
        this.priceAtBooking = priceAtBooking;
    }
}

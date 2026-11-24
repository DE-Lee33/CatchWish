
package model.dto;

import java.sql.Timestamp;

import model.domain.enums.BookingStatus;

public class BookingResponseDTO {

    private Long bookingId;
    private Long performanceId;
    private String performanceName;
    private BookingStatus bookingStatus;
    private Timestamp createdAt;

    public BookingResponseDTO() {
    }

    public BookingResponseDTO(Long bookingId, Long performanceId,
                              String performanceName,
                              BookingStatus bookingStatus,
                              Timestamp createdAt) {
        this.bookingId = bookingId;
        this.performanceId = performanceId;
        this.performanceName = performanceName;
        this.bookingStatus = bookingStatus;
        this.createdAt = createdAt;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public String getPerformanceName() {
        return performanceName;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }

    public void setPerformanceName(String performanceName) {
        this.performanceName = performanceName;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

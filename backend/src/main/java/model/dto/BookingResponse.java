
package model.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BookingResponse {

    private Long bookingId;
    private Long memberId;
    private Long performanceId;
    private String performanceName;
    private String bookingStatus;
    private Timestamp createdAt;
    private String refundAccount;

    private List<BookingSeatDTO> seats = new ArrayList<>();

    public BookingResponse() {
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public String getPerformanceName() {
        return performanceName;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getRefundAccount() {
        return refundAccount;
    }

    public List<BookingSeatDTO> getSeats() {
        return seats;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }

    public void setPerformanceName(String performanceName) {
        this.performanceName = performanceName;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setRefundAccount(String refundAccount) {
        this.refundAccount = refundAccount;
    }

    public void setSeats(List<BookingSeatDTO> seats) {
        this.seats = seats;
    }

    public void addSeat(BookingSeatDTO seat) {
        this.seats.add(seat);
    }
}

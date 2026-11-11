
package model.domain;

import java.sql.Timestamp;

public class Booking {

    private Long bookingId;
    private String bookingStatus;
    private String refundAccount;
    private Timestamp createdAt;
    private Long memberId;
    private Long paymentId;
    private Long performanceId;

    public Booking() {
    }

    public Booking(Long bookingId, String bookingStatus,
                   String refundAccount, Timestamp createdAt,
                   Long memberId, Long paymentId,
                   Long performanceId) {
        this.bookingId = bookingId;
        this.bookingStatus = bookingStatus;
        this.refundAccount = refundAccount;
        this.createdAt = createdAt;
        this.memberId = memberId;
        this.paymentId = paymentId;
        this.performanceId = performanceId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getRefundAccount() {
        return refundAccount;
    }

    public void setRefundAccount(String refundAccount) {
        this.refundAccount = refundAccount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }
}

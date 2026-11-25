package model.domain;

import java.sql.Date;

import model.domain.enums.PaymentStatus;

public class Payment {
    private long paymentId;
    private double paymentAmount;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private Date paidAt;
    private String transactionId;
    private long bookingId;
    
    public Payment() {
        
    }
    
    public Payment(long paymentId, double paymentAmount, String paymentMethod, PaymentStatus paymentStatus, Date paidAt,
            String transactionId, long bookingId) {
        super();
        this.paymentId = paymentId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.transactionId = transactionId;
        this.bookingId = bookingId;
    }

    public long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(long paymentId) {
        this.paymentId = paymentId;
    }

    public double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    @Override
    public String toString() {
        return "Payment [paymentId=" + paymentId + ", paymentAmount=" + paymentAmount + ", paymentMethod="
                + paymentMethod + ", paymentStatus=" + paymentStatus + ", paidAt=" + paidAt + ", transactionId="
                + transactionId + ", bookingId=" + bookingId + "]";
    }   
}
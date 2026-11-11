package model.dto;

public class PaymentRequestDTO {
    private long bookingId;
    private double paymentAmount;
    private String paymentMethod;
    private String transactionId;
    
    public PaymentRequestDTO(long bookingId, double paymentAmount, String paymentMethod, String transactionId) {
        this.bookingId = bookingId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public double getPaymentAmount() {
        return paymentAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }
}

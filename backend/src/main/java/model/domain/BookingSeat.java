package model.domain;

public class BookingSeat {
    private long bookingSeatId;
    private double priceAtBooking;
    private long bookingId;
    private long seatId;
    
    public BookingSeat() {
        
    }

    public BookingSeat(long bookingSeatId, double priceAtBooking, long bookingId, long seatId) {
        super();
        this.bookingSeatId = bookingSeatId;
        this.priceAtBooking = priceAtBooking;
        this.bookingId = bookingId;
        this.seatId = seatId;
    }

    public long getBookingSeatId() {
        return bookingSeatId;
    }

    public void setBookingSeatId(long bookingSeatId) {
        this.bookingSeatId = bookingSeatId;
    }

    public double getPriceAtBooking() {
        return priceAtBooking;
    }

    public void setPriceAtBooking(double priceAtBooking) {
        this.priceAtBooking = priceAtBooking;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public long getSeatId() {
        return seatId;
    }

    public void setSeatId(long seatId) {
        this.seatId = seatId;
    }

    @Override
    public String toString() {
        return "BookingSeat [bookingSeatId=" + bookingSeatId + ", priceAtBooking=" + priceAtBooking + ", bookingId="
                + bookingId + ", seatId=" + seatId + "]";
    }
}

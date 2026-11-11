package model.dto;

import java.util.ArrayList;
import java.util.List;

import model.domain.Seat;

public class SeatLayoutInfoDTO {
    private long seatLayoutId;
    private List<Seat> seats;

    public SeatLayoutInfoDTO() {
        this.seats = new ArrayList<>();
    }

    public SeatLayoutInfoDTO(long seatLayoutId, List<Seat> seats) {
        this.seatLayoutId = seatLayoutId;
        this.seats = seats;
    }

    public long getSeatLayoutId() {
        return seatLayoutId;
    }

    public void setSeatLayoutId(long seatLayoutId) {
        this.seatLayoutId = seatLayoutId;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }
}

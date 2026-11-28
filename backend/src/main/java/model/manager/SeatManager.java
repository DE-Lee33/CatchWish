package model.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.BookingSeatDAO;
import model.dao.JDBCUtil;

/** 좌석 상태 변경 담당. commit/rollback/close는 호출한 BookingManager가 한다. */
@Component
public class SeatManager {
    private final BookingSeatDAO bookingSeatDAO;

    public SeatManager(BookingSeatDAO bookingSeatDAO) {
        this.bookingSeatDAO = bookingSeatDAO;
    }

    /** 좌석 선점 + 예매 연결. 하나라도 실패하면 예외 → 호출자가 전체 롤백 */
    public void holdSeats(JDBCUtil j, Long performanceId, Long bookingId, List<Long> seatIds) throws Exception {
        List<Long> sorted = new ArrayList<>(seatIds);
        Collections.sort(sorted);                 // 데드락 방지: 항상 같은 순서로 잠금

        for (Long seatId : sorted) {
            var price = bookingSeatDAO.selectSeatPrice(j, seatId, performanceId);
            if (price == null) {
                throw new ApiException(ErrorCode.NOT_FOUND, "해당 공연의 좌석이 아닙니다.");
            }
            if (bookingSeatDAO.updateSeatToHeld(j, seatId) == 0) {
                throw new ApiException(ErrorCode.SEAT_ALREADY_HELD);
            }
            bookingSeatDAO.insertBookingSeat(j, bookingId, seatId, price.doubleValue());
        }
    }

    /** 예매의 선점중(H) 좌석을 예매가능(A)으로 복원 */
    public int releaseSeats(JDBCUtil j, Long bookingId) throws Exception {
        return bookingSeatDAO.releaseSeatsByBookingId(j, bookingId);
    }
}
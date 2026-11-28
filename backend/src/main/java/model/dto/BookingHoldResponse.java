package model.dto;

import java.util.List;

public record BookingHoldResponse(
        long bookingId,
        long performanceId,
        List<Long> seatIds,
        long holdExpiresAt,      // epoch ms
        int remainingSeconds) {  // HoldTimer는 이 값 사용 권장
}
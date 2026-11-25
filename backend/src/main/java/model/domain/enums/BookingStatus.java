package model.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingStatus {
    WAITING_PAYMENT("W"), // 선점(결제 대기)
    CONFIRMED("C"), // 확정
    CANCELED("X"); // 취소
    
    private final String code;

    BookingStatus(String code) { this.code = code; }

    @JsonValue
    public String getCode() { return code; }
    
    @JsonCreator
    public static BookingStatus fromCode(String code) {
        for (BookingStatus b : values()) {
            if (b.code.equals(code)) return b;
        }
        throw new IllegalArgumentException("잘못된 BOOKING_STATUS 코드: " + code);
    }
}
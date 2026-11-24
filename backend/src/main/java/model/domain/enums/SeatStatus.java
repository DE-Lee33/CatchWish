package model.domain.enums;

public enum SeatStatus {
    AVAILABLE("A"),       // 예매가능
    HELD("H"),            // 선점중
    BOOKED("B"),          // 예매완료
    CANCEL_PENDING("C");  // 취소대기

    private final String code;

    SeatStatus(String code) { this.code = code; }

    public String getCode() { return code; }

    public static SeatStatus fromCode(String code) {
        for (SeatStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("잘못된 SEAT_STATUS 코드: " + code);
    }
}
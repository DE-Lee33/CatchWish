package model.domain.enums;

public enum PaymentStatus {
    PENDING("P"), // 결제 대기
    COMPLETED("C"), // 결제 완료
    FAILED("F"); // 결제 실패
    
    private final String code;

    PaymentStatus(String code) { this.code = code; }

    public String getCode() { return code; }

    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus p : values()) {
            if (p.code.equals(code)) return p;
        }
        throw new IllegalArgumentException("잘못된 PAYMENT_STATUS 코드: " + code);
    }
}
package model.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SendStatus {
    NOT_SENT("N"), // 미발송
    SENT("Y"); // 발송 완료

    private final String code;

    SendStatus(String code) { this.code = code; }

    @JsonValue
    public String getCode() { return code; }

    @JsonCreator
    public static SendStatus fromCode(String code) {
        for (SendStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("잘못된 SEND_STATUS 코드: " + code);
    }
}
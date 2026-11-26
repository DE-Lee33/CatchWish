package model.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QueueStatus {
    WAITING("W"), // 대기
    TURN("A"), // 입장 차례
    EXPIRED("E"); // 만료
    
    private final String code;

    QueueStatus(String code) { this.code = code; }

    @JsonValue
    public String getCode() { return code; }

    @JsonCreator
    public static QueueStatus fromCode(String code) {
        for (QueueStatus q : values()) {
            if (q.code.equals(code)) return q;
        }
        throw new IllegalArgumentException("잘못된 QUEUE_STATUS 코드: " + code);
    }
}
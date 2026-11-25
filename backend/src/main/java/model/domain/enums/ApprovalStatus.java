package model.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApprovalStatus {
    PENDING("P"), // 심사중
    APPROVED("A"), // 승인
    REJECTED("R"); // 반려
    
    private final String code;

    ApprovalStatus(String code) { this.code = code; }

    @JsonValue
    public String getCode() { return code; }

    @JsonCreator
    public static ApprovalStatus fromCode(String code) {
        for (ApprovalStatus a : values()) {
            if (a.code.equals(code)) return a;
        }
        throw new IllegalArgumentException("잘못된 APPROVAL_STATUS 코드: " + code);
    }
}
package model.domain.enums;

public enum ScheduleStatus {
    PLANNED("P"), // 예정
    OPEN("O"), // 오픈
    CLOSED("C"); // 종료

    private final String code;

    ScheduleStatus(String code) { this.code = code; }

    public String getCode() { return code; }

    public static ScheduleStatus fromCode(String code) {
        for (ScheduleStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("잘못된 SCHEDULE_STATUS 코드: " + code);
    }
}
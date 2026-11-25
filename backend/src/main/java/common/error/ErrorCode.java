package common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 요청 방식입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    CONFLICT(HttpStatus.CONFLICT, "이미 처리되었거나 충돌이 발생했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 회원: 가입/정보 수정에서 어느 칸 오류인지 구분
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // 예매 입장 (대기열·인증)
    BOOKING_NOT_OPEN(HttpStatus.BAD_REQUEST, "아직 예매가 오픈되지 않았습니다."),
    ENTRY_REQUIRED(HttpStatus.FORBIDDEN, "예매 입장 권한이 없거나 만료되었습니다."),

    // 좌석 선점
    SEAT_ALREADY_HELD(HttpStatus.CONFLICT, "이미 다른 회원이 선택한 좌석입니다."),
    MAX_TICKETS_EXCEEDED(HttpStatus.BAD_REQUEST, "예매 가능한 매수를 초과했습니다."),

    // 결제
    HOLD_EXPIRED(HttpStatus.CONFLICT, "좌석 선점 시간이 만료되었습니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다. 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() { return status; }
    public String getDefaultMessage() { return defaultMessage; }
}
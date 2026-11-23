package common.error;

/** 모든 오류 응답의 본문: {"code": "...", "message": "..."} */
public record ApiError(String code, String message) {
}
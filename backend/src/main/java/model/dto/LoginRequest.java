package model.dto;

// POST /api/auth/login 요청 본문 {"loginId": "...", "password": "..."}
public record LoginRequest(String loginId, String password) {
}

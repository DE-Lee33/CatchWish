package model.dto;

// POST /api/members 요청 본문 {"loginId", "password", "memberName", "email"}
public record SignupRequest(String loginId, String password, String memberName, String email) {
}

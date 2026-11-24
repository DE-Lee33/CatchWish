package common.auth;

/** 세션에 저장하는 로그인 정보. 비밀번호 해시는 넣지 x. */
public record LoginMember(long memberId, String loginId, String memberName, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
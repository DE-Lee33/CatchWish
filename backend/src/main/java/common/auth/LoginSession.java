package common.auth;

import common.error.ApiException;
import common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class LoginSession {
    private static final String KEY = "LOGIN_MEMBER";
    private static final int TIMEOUT_SECONDS = 30 * 60;

    private LoginSession() {}

    /** 로그인 성공 시 호출. 기존 세션을 버리고 새 세션을 발급 (세션 고정 공격 방지). */
    public static void login(HttpServletRequest request, LoginMember member) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(TIMEOUT_SECONDS);
        session.setAttribute(KEY, member);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    /** 로그인 안 했으면 null */
    public static LoginMember get(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (LoginMember) session.getAttribute(KEY);
    }

    /** 로그인 안 했으면 401(AUTH_REQUIRED). 컨트롤러에서 memberId가 필요할 때 사용 */
    public static LoginMember require(HttpServletRequest request) {
        LoginMember member = get(request);
        if (member == null) throw new ApiException(ErrorCode.AUTH_REQUIRED);
        return member;
    }
}
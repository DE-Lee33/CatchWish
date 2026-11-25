package common.auth;

import common.error.ApiException;
import common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;   // 컨트롤러 메서드가 아닌 요청은 통과
        }

        LoginRequired required = handlerMethod.getMethodAnnotation(LoginRequired.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(LoginRequired.class);
        }
        if (required == null) {
            return true;   // @LoginRequired가 없으면 공개 API
        }

        LoginMember member = LoginSession.require(request);   // 없으면 401
        if (required.adminOnly() && !member.isAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN);       // 관리자 아니면 403
        }
        return true;
    }
}
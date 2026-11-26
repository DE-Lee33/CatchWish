package controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import common.auth.LoginMember;
import common.auth.LoginSession;
import common.error.ApiException;
import common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import model.domain.Member;
import model.dto.LoginRequest;
import model.manager.MemberManager;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final MemberManager memberManager;

	public AuthController(MemberManager memberManager) {
		this.memberManager = memberManager;
	}

	@PostMapping("/login")
	public LoginMember login(@RequestBody LoginRequest body, HttpServletRequest request) throws Exception {
		if (isEmpty(body.loginId()) || isEmpty(body.password())) {
			throw new ApiException(ErrorCode.BAD_REQUEST);
		}
		Member member = memberManager.login(body.loginId(), body.password());

		// 세션과 응답에는 비밀번호 해시가 없는 LoginMember만 담는다
		LoginMember loginMember = new LoginMember(member.getMemberId(), member.getLoginId(),
				member.getMemberName(), member.getMemberRole());
		LoginSession.login(request, loginMember);
		return loginMember;
	}

	// 이미 로그아웃된 상태여도 결과는 같으므로 실패 없이 204
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request) {
		LoginSession.logout(request);
	}

	private static boolean isEmpty(String value) {
		return value == null || value.isBlank();
	}
}

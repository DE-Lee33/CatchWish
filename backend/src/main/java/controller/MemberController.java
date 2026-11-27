package controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import common.auth.LoginMember;
import common.auth.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import model.dto.SignupRequest;
import model.manager.MemberManager;

@RestController
@RequestMapping("/api/members")
public class MemberController {
	private final MemberManager memberManager;

	public MemberController(MemberManager memberManager) {
		this.memberManager = memberManager;
	}

	// 입력 형식·중복 검사는 MemberManager.create가 한다. 가입 후 자동 로그인은 하지 않는다 (화면이 /login으로 보냄)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> register(@RequestBody SignupRequest body) throws Exception {
		long memberId = memberManager.create(body.loginId(), body.password(), body.memberName(), body.email());
		return Map.of("memberId", memberId);
	}

	// 새로고침하면 화면의 로그인 상태가 사라지므로 앱이 켜질 때 이걸 불러 복원한다
	// 세션 값을 그대로 돌려줘 DB를 조회하지 않는다 (이메일이 필요한 M3 내 정보 화면 때 DB 조회로 바꾼다)
	@GetMapping("/me")
	public LoginMember me(HttpServletRequest request) {
		return LoginSession.require(request);
	}
}

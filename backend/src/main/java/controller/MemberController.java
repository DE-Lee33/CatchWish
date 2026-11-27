package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import common.auth.LoginMember;
import common.auth.LoginSession;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/members")
public class MemberController {

	// 새로고침하면 화면의 로그인 상태가 사라지므로 앱이 켜질 때 이걸 불러 복원한다
	// 세션 값을 그대로 돌려줘 DB를 조회하지 않는다 (이메일이 필요한 M3 내 정보 화면 때 DB 조회로 바꾼다)
	@GetMapping("/me")
	public LoginMember me(HttpServletRequest request) {
		return LoginSession.require(request);
	}
}

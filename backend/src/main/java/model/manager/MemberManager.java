package model.manager;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.MemberDAO;
import model.domain.Member;

@Service
public class MemberManager {
	private final MemberDAO memberDAO;

	public MemberManager(MemberDAO memberDAO) {
		this.memberDAO = memberDAO;
	}

	public Member login(String loginId, String password) throws Exception {
		Member member = memberDAO.findByLoginId(loginId);
		// 없는 아이디와 틀린 비밀번호를 같은 오류로 응답해 가입된 아이디를 알아낼 수 없게 한다
		if (member == null || !PasswordHasher.hash(password).equals(member.getPasswordHash())) {
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}
		return member;
	}
}

package model.manager;

import java.sql.SQLException;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.MemberDAO;
import model.domain.Member;

@Service
public class MemberManager {
	// Oracle ORA-00001: UNIQUE 제약 위반
	private static final int ORACLE_UNIQUE_VIOLATION = 1;

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

	public long create(String loginId, String password, String memberName, String email) throws Exception {
		// 프론트 검사는 Postman 등으로 건너뛸 수 있어 서버에서 다시 검사한다
		if (!matches(loginId, "[A-Za-z0-9]{4,20}") || password == null || password.length() < 8
				|| memberName == null || memberName.isBlank() || !matches(email, "[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
			throw new ApiException(ErrorCode.BAD_REQUEST);
		}
		if (memberDAO.existsLoginId(loginId)) {
			throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID);
		}
		if (memberDAO.existsEmail(email, 0)) {
			throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
		}
		try {
			return memberDAO.create(new Member(loginId, PasswordHasher.hash(password), memberName, email));
		} catch (SQLException ex) {
			// 위 확인과 INSERT 사이에 누가 먼저 가입하면 DB UNIQUE(LOGIN_ID, EMAIL)가 막는다
			// 오류에 제약 이름만 나와 어느 칸인지 모르므로 아이디를 다시 조회해 구분한다
			if (ex.getErrorCode() == ORACLE_UNIQUE_VIOLATION) {
				throw new ApiException(memberDAO.existsLoginId(loginId)
						? ErrorCode.DUPLICATE_LOGIN_ID : ErrorCode.DUPLICATE_EMAIL);
			}
			throw ex;
		}
	}

	private static boolean matches(String value, String regex) {
		return value != null && value.matches(regex);
	}
}

package model.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.MemberDAO;
import model.domain.Member;

public class MemberManagerTest {

	// DB 없이 로그인 규칙만 확인하려고 조회 결과를 고정한 가짜 DAO
	private static class FakeMemberDAO extends MemberDAO {
		@Override
		public Member findByLoginId(String loginId) {
			if (!"hyunji01".equals(loginId)) {
				return null;
			}
			return new Member("hyunji01", PasswordHasher.hash("test1234"), "현지", "hyunji01@test.com");
		}
	}

	private final MemberManager memberManager = new MemberManager(new FakeMemberDAO());

	@Test
	public void 아이디와_비밀번호가_맞으면_회원을_돌려준다() throws Exception {
		Member member = memberManager.login("hyunji01", "test1234");
		assertEquals("hyunji01", member.getLoginId());
	}

	@Test
	public void 비밀번호가_틀리면_LOGIN_FAILED() {
		assertLoginFailed("hyunji01", "test9999");
	}

	@Test
	public void 없는_아이디도_같은_LOGIN_FAILED() {
		assertLoginFailed("nobody", "test1234");
	}

	@Test
	public void 해시값을_비밀번호로_넣으면_LOGIN_FAILED() {
		assertLoginFailed("hyunji01", PasswordHasher.hash("test1234"));
	}

	private void assertLoginFailed(String loginId, String password) {
		ApiException ex = assertThrows(ApiException.class, () -> memberManager.login(loginId, password));
		assertEquals(ErrorCode.LOGIN_FAILED, ex.getErrorCode());
	}
}

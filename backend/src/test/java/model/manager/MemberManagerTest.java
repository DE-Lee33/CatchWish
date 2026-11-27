package model.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.MemberDAO;
import model.domain.Member;

public class MemberManagerTest {

	// DB 없이 로그인·가입 규칙만 확인하려고 hyunji01 한 명만 가입된 상태로 고정한 가짜 DAO
	private static class FakeMemberDAO extends MemberDAO {
		private Member created;

		@Override
		public Member findByLoginId(String loginId) {
			if (!"hyunji01".equals(loginId)) {
				return null;
			}
			return new Member("hyunji01", PasswordHasher.hash("test1234"), "현지", "hyunji01@test.com");
		}

		@Override
		public boolean existsLoginId(String loginId) {
			return "hyunji01".equals(loginId);
		}

		@Override
		public boolean existsEmail(String email, long excludeMemberId) {
			return "hyunji01@test.com".equals(email);
		}

		@Override
		public long create(Member member) {
			created = member;
			return 100;
		}
	}

	// 중복 확인은 통과했는데 그 사이 다른 사람이 먼저 가입해 INSERT가 UNIQUE 위반으로 실패하는 상황
	private static class RaceMemberDAO extends MemberDAO {
		private final boolean loginIdTaken;
		private boolean inserted;

		RaceMemberDAO(boolean loginIdTaken) {
			this.loginIdTaken = loginIdTaken;
		}

		@Override
		public boolean existsLoginId(String loginId) {
			return inserted && loginIdTaken;
		}

		@Override
		public boolean existsEmail(String email, long excludeMemberId) {
			return false;
		}

		@Override
		public long create(Member member) throws SQLException {
			inserted = true;
			throw new SQLException("ORA-00001: unique constraint violated", "23000", 1);
		}
	}

	private final FakeMemberDAO memberDAO = new FakeMemberDAO();
	private final MemberManager memberManager = new MemberManager(memberDAO);

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

	@Test
	public void 중복확인은_사용_중인_아이디면_true() throws Exception {
		assertTrue(memberManager.existsLoginId("hyunji01"));
		assertFalse(memberManager.existsLoginId("newbie01"));
	}

	@Test
	public void 중복확인_아이디_형식이_틀리면_BAD_REQUEST() {
		assertErrorCode(ErrorCode.BAD_REQUEST, () -> memberManager.existsLoginId("ab"));
		assertErrorCode(ErrorCode.BAD_REQUEST, () -> memberManager.existsLoginId(null));
	}

	@Test
	public void 가입하면_해시한_비밀번호로_저장하고_회원번호를_돌려준다() throws Exception {
		long memberId = memberManager.create("newbie01", "password1", "새회원", "newbie@test.com");

		assertEquals(100, memberId);
		assertEquals("newbie01", memberDAO.created.getLoginId());
		assertEquals(PasswordHasher.hash("password1"), memberDAO.created.getPasswordHash());
	}

	@Test
	public void 형식이_틀리면_BAD_REQUEST() {
		assertCreateFails(ErrorCode.BAD_REQUEST, memberManager, "abc", "password1", "새회원", "a@test.com");
		assertCreateFails(ErrorCode.BAD_REQUEST, memberManager, "newbie_01", "password1", "새회원", "a@test.com");
		assertCreateFails(ErrorCode.BAD_REQUEST, memberManager, "newbie01", "pass123", "새회원", "a@test.com");
		assertCreateFails(ErrorCode.BAD_REQUEST, memberManager, "newbie01", "password1", " ", "a@test.com");
		assertCreateFails(ErrorCode.BAD_REQUEST, memberManager, "newbie01", "password1", "새회원", "a@test");
		assertCreateFails(ErrorCode.BAD_REQUEST, memberManager, null, null, null, null);
	}

	@Test
	public void 아이디가_중복이면_DUPLICATE_LOGIN_ID() {
		assertCreateFails(ErrorCode.DUPLICATE_LOGIN_ID, memberManager, "hyunji01", "password1", "새회원", "a@test.com");
	}

	@Test
	public void 이메일이_중복이면_DUPLICATE_EMAIL() {
		assertCreateFails(ErrorCode.DUPLICATE_EMAIL, memberManager, "newbie01", "password1", "새회원", "hyunji01@test.com");
	}

	@Test
	public void 동시_가입으로_아이디가_겹치면_DUPLICATE_LOGIN_ID() {
		assertCreateFails(ErrorCode.DUPLICATE_LOGIN_ID, new MemberManager(new RaceMemberDAO(true)),
				"newbie01", "password1", "새회원", "a@test.com");
	}

	@Test
	public void 동시_가입으로_이메일이_겹치면_DUPLICATE_EMAIL() {
		assertCreateFails(ErrorCode.DUPLICATE_EMAIL, new MemberManager(new RaceMemberDAO(false)),
				"newbie01", "password1", "새회원", "a@test.com");
	}

	private void assertLoginFailed(String loginId, String password) {
		assertErrorCode(ErrorCode.LOGIN_FAILED, () -> memberManager.login(loginId, password));
	}

	private void assertCreateFails(ErrorCode expected, MemberManager manager,
			String loginId, String password, String memberName, String email) {
		assertErrorCode(expected, () -> manager.create(loginId, password, memberName, email));
	}

	private void assertErrorCode(ErrorCode expected, ThrowingRunnable action) {
		ApiException ex = assertThrows(ApiException.class, action);
		assertEquals(expected, ex.getErrorCode());
	}
}

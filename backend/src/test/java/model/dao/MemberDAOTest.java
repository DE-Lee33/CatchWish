package model.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.domain.Member;

/**
 * MemberDAO 테스트
 * 팀 공용 DB를 쓰므로, 테스트에서 만든 회원은 끝날 때 반드시 지운다.
 */
public class MemberDAOTest {
	private MemberDAO memberDAO;
	private String loginId;
	private long createdMemberId = 0;

	@Before
	public void setUp() {
		assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
				getClass().getClassLoader().getResource("db.properties") != null);
		memberDAO = new MemberDAO();
		// LOGIN_ID는 VARCHAR2(30)이므로 짧게, 다른 테스트와 겹치지 않게
		loginId = "t_mem_" + (System.currentTimeMillis() % 100000000L);
	}

	@After
	public void tearDown() throws Exception {
		if (createdMemberId == 0) {
			return;
		}
		JDBCUtil jdbcUtil = new JDBCUtil();
		try {
			jdbcUtil.setSqlAndParameters("DELETE FROM MEMBER WHERE MEMBER_ID = ?", new Object[] { createdMemberId });
			jdbcUtil.executeUpdate();
			jdbcUtil.commit();
		} finally {
			jdbcUtil.close();
		}
	}

	@Test
	public void createAndFind() throws Exception {
		assertFalse(memberDAO.existsLoginId(loginId));

		Member member = new Member(loginId, "hash_1234", "테스트회원", loginId + "@test.com");
		createdMemberId = memberDAO.create(member);
		assertTrue(createdMemberId > 0);
		assertTrue(memberDAO.existsLoginId(loginId));

		Member byLoginId = memberDAO.findByLoginId(loginId);
		assertNotNull(byLoginId);
		assertEquals(createdMemberId, byLoginId.getMemberId());
		assertEquals("hash_1234", byLoginId.getPasswordHash());
		assertEquals(Member.ROLE_USER, byLoginId.getMemberRole());
		assertNotNull(byLoginId.getCreatedAt());

		Member byId = memberDAO.findById(createdMemberId);
		assertNotNull(byId);
		assertEquals(loginId, byId.getLoginId());

		assertNull(memberDAO.findByLoginId(loginId + "_none"));
	}

	@Test
	public void existsEmailExcludesSelf() throws Exception {
		String email = loginId + "@test.com";
		createdMemberId = memberDAO.create(new Member(loginId, "hash", "테스트회원", email));

		// 회원가입 시(0)에는 중복, 본인 수정 시에는 중복 아님
		assertTrue(memberDAO.existsEmail(email, 0));
		assertFalse(memberDAO.existsEmail(email, createdMemberId));
	}

	@Test
	public void updateNameEmailAndPassword() throws Exception {
		Member member = new Member(loginId, "old_hash", "변경전", loginId + "@test.com");
		createdMemberId = memberDAO.create(member);

		member.setMemberName("변경후");
		member.setEmail(loginId + "@new.com");
		assertEquals(1, memberDAO.update(member));
		assertEquals(1, memberDAO.updatePassword(createdMemberId, "new_hash"));

		Member updated = memberDAO.findById(createdMemberId);
		assertEquals("변경후", updated.getMemberName());
		assertEquals(loginId + "@new.com", updated.getEmail());
		assertEquals("new_hash", updated.getPasswordHash());
	}
}

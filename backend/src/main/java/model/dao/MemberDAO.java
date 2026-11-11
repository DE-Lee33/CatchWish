package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import model.domain.Member;

/**
 * 회원 DAO (MEMBER 테이블)
 * 담당 기능: M1 회원가입·아이디 중복확인, M2 로그인, M3-1 회원정보 조회, M3-2 회원정보 수정
 */
public class MemberDAO {
	private static final String SELECT_COLUMNS =
			"SELECT MEMBER_ID, LOGIN_ID, PASSWORD_HASH, MEMBER_NAME, EMAIL, MEMBER_ROLE, CREATED_AT FROM MEMBER ";

	private JDBCUtil jdbcUtil = null;

	public MemberDAO() {
		jdbcUtil = new JDBCUtil();
	}

	/** M1 아이디 중복확인: 이미 사용 중이면 true */
	public boolean existsLoginId(String loginId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM MEMBER WHERE LOGIN_ID = ?";
		return queryCount(sql, new Object[] { loginId }) > 0;
	}

	/**
	 * M1, M3-2 이메일 중복확인: 이미 사용 중이면 true
	 * @param excludeMemberId 정보 수정 시 본인 회원 번호 (회원가입 시에는 0)
	 */
	public boolean existsEmail(String email, long excludeMemberId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM MEMBER WHERE EMAIL = ? AND MEMBER_ID <> ?";
		return queryCount(sql, new Object[] { email, excludeMemberId }) > 0;
	}

	/** M1 회원가입: 생성된 회원 번호를 반환 (역할은 항상 USER) */
	public long create(Member member) throws SQLException {
		String sql = "INSERT INTO MEMBER (MEMBER_ID, LOGIN_ID, PASSWORD_HASH, MEMBER_NAME, EMAIL, MEMBER_ROLE, CREATED_AT) "
				+ "VALUES (?, ?, ?, ?, ?, 'USER', SYSDATE)";
		try {
			long memberId = nextSequence("SEQ_MEMBER");
			jdbcUtil.setSqlAndParameters(sql, new Object[] {
					memberId, member.getLoginId(), member.getPasswordHash(),
					member.getMemberName(), member.getEmail() });
			jdbcUtil.executeUpdate();
			jdbcUtil.commit();

			member.setMemberId(memberId);
			member.setMemberRole(Member.ROLE_USER);
			return memberId;
		} catch (Exception ex) {
			rollbackQuietly();
			throw toSQLException("회원 등록 실패", ex);
		} finally {
			jdbcUtil.close();
		}
	}

	/** M2 로그인용 조회: 없으면 null (비밀번호 비교는 Manager에서) */
	public Member findByLoginId(String loginId) throws SQLException {
		return findOne(SELECT_COLUMNS + "WHERE LOGIN_ID = ?", new Object[] { loginId });
	}

	/** M3-1 회원정보 조회: 없으면 null */
	public Member findById(long memberId) throws SQLException {
		return findOne(SELECT_COLUMNS + "WHERE MEMBER_ID = ?", new Object[] { memberId });
	}

	/** M3-2 이름·이메일 수정: 변경된 행 수를 반환 */
	public int update(Member member) throws SQLException {
		String sql = "UPDATE MEMBER SET MEMBER_NAME = ?, EMAIL = ? WHERE MEMBER_ID = ?";
		return executeChange(sql, new Object[] {
				member.getMemberName(), member.getEmail(), member.getMemberId() });
	}

	/** M3-2 비밀번호 변경: 변경된 행 수를 반환 */
	public int updatePassword(long memberId, String passwordHash) throws SQLException {
		String sql = "UPDATE MEMBER SET PASSWORD_HASH = ? WHERE MEMBER_ID = ?";
		return executeChange(sql, new Object[] { passwordHash, memberId });
	}

	// ===================== 내부 공통 처리 =====================

	private Member findOne(String sql, Object[] params) throws SQLException {
		jdbcUtil.setSqlAndParameters(sql, params);
		try {
			ResultSet rs = executeQueryOrThrow();
			if (rs.next()) {
				return mapMember(rs);
			}
			return null;
		} finally {
			jdbcUtil.close();
		}
	}

	private Member mapMember(ResultSet rs) throws SQLException {
		Member member = new Member();
		member.setMemberId(rs.getLong("MEMBER_ID"));
		member.setLoginId(rs.getString("LOGIN_ID"));
		member.setPasswordHash(rs.getString("PASSWORD_HASH"));
		member.setMemberName(rs.getString("MEMBER_NAME"));
		member.setEmail(rs.getString("EMAIL"));
		member.setMemberRole(rs.getString("MEMBER_ROLE"));
		member.setCreatedAt(rs.getTimestamp("CREATED_AT"));
		return member;
	}

	private int queryCount(String sql, Object[] params) throws SQLException {
		jdbcUtil.setSqlAndParameters(sql, params);
		try {
			ResultSet rs = executeQueryOrThrow();
			rs.next();
			return rs.getInt(1);
		} finally {
			jdbcUtil.close();
		}
	}

	/** INSERT·UPDATE·DELETE 공통: 성공하면 commit, 실패하면 rollback */
	private int executeChange(String sql, Object[] params) throws SQLException {
		jdbcUtil.setSqlAndParameters(sql, params);
		try {
			int result = jdbcUtil.executeUpdate();
			jdbcUtil.commit();
			return result;
		} catch (Exception ex) {
			rollbackQuietly();
			throw toSQLException("회원 정보 변경 실패", ex);
		} finally {
			jdbcUtil.close();
		}
	}

	/** 시퀀스 다음 값 조회 (close는 호출한 쪽에서) */
	private long nextSequence(String sequenceName) throws SQLException {
		jdbcUtil.setSqlAndParameters("SELECT " + sequenceName + ".NEXTVAL FROM DUAL", null);
		ResultSet rs = executeQueryOrThrow();
		if (!rs.next()) {
			throw new SQLException(sequenceName + " 값을 가져오지 못했습니다.");
		}
		return rs.getLong(1);
	}

	/** JDBCUtil.executeQuery()는 실패하면 null을 반환하므로 예외로 바꿔서 알린다 */
	private ResultSet executeQueryOrThrow() throws SQLException {
		ResultSet rs = jdbcUtil.executeQuery();
		if (rs == null) {
			throw new SQLException("쿼리 실행 실패: " + jdbcUtil.getSql());
		}
		return rs;
	}

	/** 연결을 얻기 전에 실패한 경우에도 rollback에서 다른 예외가 나지 않게 막는다 */
	private void rollbackQuietly() {
		try {
			jdbcUtil.rollback();
		} catch (Exception ignore) {
			// 원래 예외를 그대로 전달하기 위해 무시
		}
	}

	private SQLException toSQLException(String message, Exception ex) {
		if (ex instanceof SQLException) {
			return (SQLException) ex;
		}
		return new SQLException(message, ex);
	}
}

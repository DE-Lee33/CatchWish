package model.domain;

import java.util.Date;

/**
 * 회원 (MEMBER 테이블)
 * 일반 회원과 관리자를 MEMBER_ROLE로 구분한다.
 */
public class Member {
	public static final String ROLE_USER = "USER";
	public static final String ROLE_ADMIN = "ADMIN";

	private long memberId;
	private String loginId;
	private String passwordHash;
	private String memberName;
	private String email;
	private String memberRole;
	private Date createdAt;

	public Member() {
	}

	/** 회원가입용 생성자 (가입 시 역할은 항상 USER) */
	public Member(String loginId, String passwordHash, String memberName, String email) {
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.memberName = memberName;
		this.email = email;
		this.memberRole = ROLE_USER;
	}

	public boolean isAdmin() {
		return ROLE_ADMIN.equals(memberRole);
	}

	public long getMemberId() {
		return memberId;
	}

	public void setMemberId(long memberId) {
		this.memberId = memberId;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMemberRole() {
		return memberRole;
	}

	public void setMemberRole(String memberRole) {
		this.memberRole = memberRole;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		// 비밀번호 해시는 로그에 남기지 않는다
		return "Member [memberId=" + memberId + ", loginId=" + loginId + ", memberName=" + memberName
				+ ", email=" + email + ", memberRole=" + memberRole + ", createdAt=" + createdAt + "]";
	}
}

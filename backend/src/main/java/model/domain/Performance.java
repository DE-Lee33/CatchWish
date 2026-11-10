package model.domain;

import java.util.Date;

/**
 * 공연 (PERFORMANCE 테이블)
 * 공연 기본 정보 + 관리자 심사 상태를 함께 가진다.
 */
public class Performance {
	/** 심사 상태 코드 (docs/STATUS_CODES.md) */
	public static final String STATUS_PENDING = "P";   // 심사중
	public static final String STATUS_APPROVED = "A";  // 승인
	public static final String STATUS_REJECTED = "R";  // 반려

	private long performanceId;
	private long registrantId;        // 공연 등록자 (MEMBER_ID)
	private Long managerId;           // 심사한 관리자 (심사 전에는 null)
	private Long venueTemplateId;     // 사용한 공연장 템플릿 (직접 그렸으면 null)
	private String performanceName;
	private String genre;
	private String castInfo;
	private String description;
	private String posterUrl;
	private int runningTime;          // 분
	private Date showAt;              // 공연 일시
	private Date bookingOpenAt;       // 예매 오픈 시각
	private Date cancelDeadlineAt;    // 예매 취소 가능 기한
	private int maxTicketsPerMember;  // 1인 최대 예매 매수
	private String venueName;
	private String venueAddress;
	private String approvalStatus;
	private String rejectReason;
	private Date reviewedAt;
	private Date createdAt;

	public Performance() {
	}

	public boolean isApproved() {
		return STATUS_APPROVED.equals(approvalStatus);
	}

	public long getPerformanceId() {
		return performanceId;
	}

	public void setPerformanceId(long performanceId) {
		this.performanceId = performanceId;
	}

	public long getRegistrantId() {
		return registrantId;
	}

	public void setRegistrantId(long registrantId) {
		this.registrantId = registrantId;
	}

	public Long getManagerId() {
		return managerId;
	}

	public void setManagerId(Long managerId) {
		this.managerId = managerId;
	}

	public Long getVenueTemplateId() {
		return venueTemplateId;
	}

	public void setVenueTemplateId(Long venueTemplateId) {
		this.venueTemplateId = venueTemplateId;
	}

	public String getPerformanceName() {
		return performanceName;
	}

	public void setPerformanceName(String performanceName) {
		this.performanceName = performanceName;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getCastInfo() {
		return castInfo;
	}

	public void setCastInfo(String castInfo) {
		this.castInfo = castInfo;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public void setPosterUrl(String posterUrl) {
		this.posterUrl = posterUrl;
	}

	public int getRunningTime() {
		return runningTime;
	}

	public void setRunningTime(int runningTime) {
		this.runningTime = runningTime;
	}

	public Date getShowAt() {
		return showAt;
	}

	public void setShowAt(Date showAt) {
		this.showAt = showAt;
	}

	public Date getBookingOpenAt() {
		return bookingOpenAt;
	}

	public void setBookingOpenAt(Date bookingOpenAt) {
		this.bookingOpenAt = bookingOpenAt;
	}

	public Date getCancelDeadlineAt() {
		return cancelDeadlineAt;
	}

	public void setCancelDeadlineAt(Date cancelDeadlineAt) {
		this.cancelDeadlineAt = cancelDeadlineAt;
	}

	public int getMaxTicketsPerMember() {
		return maxTicketsPerMember;
	}

	public void setMaxTicketsPerMember(int maxTicketsPerMember) {
		this.maxTicketsPerMember = maxTicketsPerMember;
	}

	public String getVenueName() {
		return venueName;
	}

	public void setVenueName(String venueName) {
		this.venueName = venueName;
	}

	public String getVenueAddress() {
		return venueAddress;
	}

	public void setVenueAddress(String venueAddress) {
		this.venueAddress = venueAddress;
	}

	public String getApprovalStatus() {
		return approvalStatus;
	}

	public void setApprovalStatus(String approvalStatus) {
		this.approvalStatus = approvalStatus;
	}

	public String getRejectReason() {
		return rejectReason;
	}

	public void setRejectReason(String rejectReason) {
		this.rejectReason = rejectReason;
	}

	public Date getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(Date reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "Performance [performanceId=" + performanceId + ", performanceName=" + performanceName
				+ ", genre=" + genre + ", showAt=" + showAt + ", venueName=" + venueName
				+ ", approvalStatus=" + approvalStatus + ", registrantId=" + registrantId + "]";
	}
}

package model.domain;

import java.util.Date;

/**
 * 공연 등록 시 제출한 대관 인증 자료 (VENUE_RENTAL_PROOF 테이블)
 * 공연 1개당 1개.
 */
public class VenueRentalProof {
	private long proofId;
	private long performanceId;
	private String filePath;          // 서버 저장 경로
	private String originalFileName;  // 업로드 원본 파일명
	private Date rentalStartAt;
	private Date rentalEndAt;
	private Date submittedAt;

	public VenueRentalProof() {
	}

	/**
	 * 공연 일시가 대관 기간 안에 있는지 확인
	 * (두 테이블에 걸친 조건이라 CHECK 제약으로 못 걸어서 Manager에서 이 메서드로 확인)
	 */
	public boolean covers(Date showAt) {
		if (showAt == null || rentalStartAt == null || rentalEndAt == null) {
			return false;
		}
		return !showAt.before(rentalStartAt) && !showAt.after(rentalEndAt);
	}

	public long getProofId() {
		return proofId;
	}

	public void setProofId(long proofId) {
		this.proofId = proofId;
	}

	public long getPerformanceId() {
		return performanceId;
	}

	public void setPerformanceId(long performanceId) {
		this.performanceId = performanceId;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public Date getRentalStartAt() {
		return rentalStartAt;
	}

	public void setRentalStartAt(Date rentalStartAt) {
		this.rentalStartAt = rentalStartAt;
	}

	public Date getRentalEndAt() {
		return rentalEndAt;
	}

	public void setRentalEndAt(Date rentalEndAt) {
		this.rentalEndAt = rentalEndAt;
	}

	public Date getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Date submittedAt) {
		this.submittedAt = submittedAt;
	}

	@Override
	public String toString() {
		return "VenueRentalProof [proofId=" + proofId + ", performanceId=" + performanceId
				+ ", originalFileName=" + originalFileName + ", rentalStartAt=" + rentalStartAt
				+ ", rentalEndAt=" + rentalEndAt + "]";
	}
}

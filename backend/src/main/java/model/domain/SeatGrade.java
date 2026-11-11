package model.domain;

/**
 * 공연별 좌석 등급과 가격 (SEAT_GRADE 테이블)
 * 섹터(SECTOR)가 이 등급을 참조한다.
 */
public class SeatGrade {
	private long gradeId;
	private long performanceId;
	private String gradeName;   // VIP, R, S ...
	private int price;          // 원

	public SeatGrade() {
	}

	public SeatGrade(long performanceId, String gradeName, int price) {
		this.performanceId = performanceId;
		this.gradeName = gradeName;
		this.price = price;
	}

	public long getGradeId() {
		return gradeId;
	}

	public void setGradeId(long gradeId) {
		this.gradeId = gradeId;
	}

	public long getPerformanceId() {
		return performanceId;
	}

	public void setPerformanceId(long performanceId) {
		this.performanceId = performanceId;
	}

	public String getGradeName() {
		return gradeName;
	}

	public void setGradeName(String gradeName) {
		this.gradeName = gradeName;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	@Override
	public String toString() {
		return "SeatGrade [gradeId=" + gradeId + ", performanceId=" + performanceId
				+ ", gradeName=" + gradeName + ", price=" + price + "]";
	}
}

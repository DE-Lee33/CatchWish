package model.dto;

import model.domain.Performance;

/**
 * M8 마이페이지 "내가 등록한 공연" 한 줄
 * 공연 정보 + 좌석 집계(전체 좌석 수, 잔여석 수)를 함께 담는다.
 * 좌석 수는 SECTOR·SEAT 테이블을 집계한 값이라 Performance 도메인 클래스로는 표현할 수 없어서 따로 만든다.
 */
public class MyPerformanceSummary {
	private Performance performance;
	private int totalSeats;
	private int availableSeats;

	public MyPerformanceSummary() {
	}

	public MyPerformanceSummary(Performance performance, int totalSeats, int availableSeats) {
		this.performance = performance;
		this.totalSeats = totalSeats;
		this.availableSeats = availableSeats;
	}

	public Performance getPerformance() {
		return performance;
	}

	public void setPerformance(Performance performance) {
		this.performance = performance;
	}

	public int getTotalSeats() {
		return totalSeats;
	}

	public void setTotalSeats(int totalSeats) {
		this.totalSeats = totalSeats;
	}

	public int getAvailableSeats() {
		return availableSeats;
	}

	public void setAvailableSeats(int availableSeats) {
		this.availableSeats = availableSeats;
	}

	@Override
	public String toString() {
		return "MyPerformanceSummary [performance=" + performance + ", totalSeats=" + totalSeats
				+ ", availableSeats=" + availableSeats + "]";
	}
}

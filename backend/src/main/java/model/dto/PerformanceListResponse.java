package model.dto;

import java.util.List;

import model.domain.Performance;

// GET /api/performances 응답. totalCount는 화면이 페이지 버튼 수를 계산하는 데 쓴다
public record PerformanceListResponse(List<Performance> performanceList, int totalCount) {
}

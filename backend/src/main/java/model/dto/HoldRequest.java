package model.dto;

import java.util.List;

public record HoldRequest(Long performanceId, List<Long> seatIds) { }
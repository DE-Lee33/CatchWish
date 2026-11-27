package model.dto;

// POST /api/wishlist 요청 본문. sectorId와 priority는 비워도 됨
public record WishlistRequest(Long performanceId, Long sectorId, Integer priority) {
}

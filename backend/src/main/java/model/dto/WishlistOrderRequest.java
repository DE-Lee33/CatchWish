package model.dto;

import java.util.List;

// PUT /api/wishlist/order 요청 본문. 화면에 보이는 순서대로 위시리스트 ID 전체를 보낸다
public record WishlistOrderRequest(List<Long> wishlistIds) {
}
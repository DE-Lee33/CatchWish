
package model.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import model.domain.Wishlist;
import model.dto.WishlistResponseDTO;

public class WishlistDAOTest {

    private WishlistDAO wishlistDAO;

    @Before
    public void setUp() {
        wishlistDAO = new WishlistDAO();
    }

    // 회원의 위시리스트 조회 테스트
    @Test
    public void testSelectWishlistByMemberId() throws Exception {
        Long memberId = Long.MAX_VALUE;

        List<WishlistResponseDTO> result =
                wishlistDAO.selectWishlistByMemberId(memberId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // 위시리스트 삭제 테스트
    @Test
    public void testDeleteWishlist() throws Exception {
        Long wishlistId = Long.MAX_VALUE;
        Long memberId = Long.MAX_VALUE;

        int result = wishlistDAO.deleteWishlist(wishlistId, memberId);

        assertEquals(0, result);
    }

    // 위시리스트 우선순위 변경 테스트
    @Test
    public void testUpdatePriority() throws Exception {
        Long wishlistId = Long.MAX_VALUE;
        Long memberId = Long.MAX_VALUE;
        int priority = 1;

        int result =
                wishlistDAO.updatePriority(wishlistId, memberId, priority);

        assertEquals(0, result);
    }

    // NULL 처리 테스트
    @Test
    public void testWishlistNullPriority() {
        Wishlist wishlist = new Wishlist(
                1L, 1L, 1L, 1L, null, null
        );

        assertNull(wishlist.getPriority());
    }
}

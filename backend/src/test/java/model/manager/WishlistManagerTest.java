package model.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.WishlistDAO;
import model.domain.Wishlist;
import model.dto.WishlistResponseDTO;

public class WishlistManagerTest {

    // DB 없이 Manager 규칙만 확인하려고 메모리에서 동작하는 가짜 DAO
    private static class FakeWishlistDAO extends WishlistDAO {
        final List<WishlistResponseDTO> rows = new ArrayList<>();
        SQLException insertError;
        int deleteResult = 1;
        int updateResult = 1;

        FakeWishlistDAO() {
            super(null);
        }

        @Override
        public int insertWishlist(Long memberId, Long performanceId, Long sectorId, Integer priority)
                throws SQLException {
            if (insertError != null) {
                throw insertError;
            }
            Wishlist wishlist = new Wishlist(100L + rows.size(), memberId, performanceId, sectorId, priority, null);
            rows.add(new WishlistResponseDTO(wishlist, "공연", null, null));
            return 1;
        }

        @Override
        public List<WishlistResponseDTO> selectWishlistByMemberId(Long memberId) {
            return rows;
        }

        @Override
        public int deleteWishlist(Long wishlistId, Long memberId) {
            return deleteResult;
        }

        @Override
        public int updatePriority(Long wishlistId, Long memberId, int priority) {
            return updateResult;
        }
    }

    private final FakeWishlistDAO dao = new FakeWishlistDAO();
    private final WishlistManager manager = new WishlistManager(dao);

    private static void assertErrorCode(ErrorCode expected, org.junit.function.ThrowingRunnable action) {
        ApiException ex = assertThrows(ApiException.class, action);
        assertEquals(expected, ex.getErrorCode());
    }

    @Test
    public void 등록하면_방금_등록한_항목을_돌려준다() throws Exception {
        WishlistResponseDTO created = manager.create(1L, 10L, null, 2);

        assertEquals(Long.valueOf(10L), created.getWishlist().getPerformanceId());
        assertEquals(Integer.valueOf(2), created.getWishlist().getPriority());
    }

    @Test
    public void 같은_공연_구역을_다시_등록하면_CONFLICT() throws Exception {
        manager.create(1L, 10L, 5L, null);

        assertErrorCode(ErrorCode.CONFLICT, () -> manager.create(1L, 10L, 5L, null));
    }

    @Test
    public void 같은_공연이라도_구역이_다르면_등록된다() throws Exception {
        manager.create(1L, 10L, 5L, null);
        manager.create(1L, 10L, 6L, null);

        assertEquals(2, dao.rows.size());
    }

    @Test
    public void 공연이_없으면_BAD_REQUEST() {
        assertErrorCode(ErrorCode.BAD_REQUEST, () -> manager.create(1L, null, null, null));
    }

    @Test
    public void 우선순위가_1보다_작으면_BAD_REQUEST() {
        assertErrorCode(ErrorCode.BAD_REQUEST, () -> manager.create(1L, 10L, null, 0));
    }

    @Test
    public void 없는_공연이면_NOT_FOUND() {
        dao.insertError = new SQLException("parent key not found", "23000", 2291);

        assertErrorCode(ErrorCode.NOT_FOUND, () -> manager.create(1L, 999L, null, null));
    }

    @Test
    public void 삭제할_항목이_없으면_NOT_FOUND() {
        dao.deleteResult = 0;

        assertErrorCode(ErrorCode.NOT_FOUND, () -> manager.remove(100L, 1L));
    }

    @Test
    public void 우선순위를_바꾸면_목록을_돌려준다() throws Exception {
        manager.create(1L, 10L, null, 1);

        assertEquals(1, manager.updatePriority(100L, 1L, 3).size());
    }

    @Test
    public void 우선순위_변경_대상이_없으면_NOT_FOUND() {
        dao.updateResult = 0;

        assertErrorCode(ErrorCode.NOT_FOUND, () -> manager.updatePriority(100L, 1L, 2));
    }

    @Test
    public void 우선순위가_비어_있으면_BAD_REQUEST() {
        assertErrorCode(ErrorCode.BAD_REQUEST, () -> manager.updatePriority(100L, 1L, null));
    }
}

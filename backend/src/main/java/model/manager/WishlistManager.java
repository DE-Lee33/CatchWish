package model.manager;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.WishlistDAO;
import model.dto.WishlistResponseDTO;

@Service
public class WishlistManager {

    // 부모 키(공연·구역)가 없을 때 Oracle이 던지는 오류 번호 (ORA-02291)
    private static final int ORA_PARENT_KEY_NOT_FOUND = 2291;

    private final WishlistDAO wishlistDAO;

    public WishlistManager(WishlistDAO wishlistDAO) {
        this.wishlistDAO = wishlistDAO;
    }

    /** 위시 등록. 같은 공연·구역 조합은 한 번만 등록할 수 있다. */
    public WishlistResponseDTO create(Long memberId, Long performanceId, Long sectorId, Integer priority)
            throws Exception {
        if (performanceId == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "공연을 선택해주세요.");
        }
        if (priority != null && priority < 1) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "우선순위는 1 이상이어야 합니다.");
        }
        if (findSame(wishlistDAO.selectWishlistByMemberId(memberId), performanceId, sectorId) != null) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 위시리스트에 등록된 공연·구역입니다.");
        }

        try {
            wishlistDAO.insertWishlist(memberId, performanceId, sectorId, priority);
        } catch (SQLException ex) {
            if (ex.getErrorCode() == ORA_PARENT_KEY_NOT_FOUND) {
                throw new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 공연 또는 구역입니다.");
            }
            throw ex;
        }

        // DAO가 생성된 ID를 돌려주지 않으므로, 방금 등록한 항목을 목록에서 찾아 응답한다
        WishlistResponseDTO created = findSame(wishlistDAO.selectWishlistByMemberId(memberId), performanceId, sectorId);
        if (created == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
        return created;
    }

    public List<WishlistResponseDTO> findWishlistList(Long memberId) throws Exception {
        return wishlistDAO.selectWishlistByMemberId(memberId);
    }

    /** 본인 항목만 삭제된다. 없는 항목과 남의 항목은 구분하지 않고 NOT_FOUND */
    public void remove(Long wishlistId, Long memberId) throws Exception {
        if (wishlistDAO.deleteWishlist(wishlistId, memberId) == 0) {
            throw new ApiException(ErrorCode.NOT_FOUND, "위시리스트 항목을 찾을 수 없습니다.");
        }
    }

    public List<WishlistResponseDTO> updatePriority(Long wishlistId, Long memberId, Integer priority)
            throws Exception {
        if (priority == null || priority < 1) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "우선순위는 1 이상이어야 합니다.");
        }
        if (wishlistDAO.updatePriority(wishlistId, memberId, priority) == 0) {
            throw new ApiException(ErrorCode.NOT_FOUND, "위시리스트 항목을 찾을 수 없습니다.");
        }
        return wishlistDAO.selectWishlistByMemberId(memberId);
    }

    private static WishlistResponseDTO findSame(List<WishlistResponseDTO> list, Long performanceId, Long sectorId) {
        for (WishlistResponseDTO item : list) {
            if (Objects.equals(item.getWishlist().getPerformanceId(), performanceId)
                    && Objects.equals(item.getWishlist().getSectorId(), sectorId)) {
                return item;
            }
        }
        return null;
    }
}

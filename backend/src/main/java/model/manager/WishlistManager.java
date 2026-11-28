package model.manager;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.JDBCUtil;
import model.dao.WishlistDAO;
import model.dto.WishlistResponseDTO;

@Service
public class WishlistManager {
    // 부모 키(공연·구역)가 없을 때 Oracle이 던지는 오류 번호 (ORA-02291)
    private static final int ORA_PARENT_KEY_NOT_FOUND = 2291;
    // 유니크 제약 위반 (ORA-00001)
    private static final int ORA_UNIQUE_VIOLATED = 1;

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
        // 다른 공연의 구역을 넣으면 이후 알림·위시 좌석 매칭(M28, M33)이 어긋나므로 막는다
        if (sectorId != null && !wishlistDAO.existsSectorInPerformance(sectorId, performanceId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 공연 또는 구역입니다.");
        }
        // 친절한 오류용 사전 확인. 동시 요청은 DB 유니크 제약이 최종적으로 막는다
        if (findSame(wishlistDAO.selectWishlistByMemberId(memberId), performanceId, sectorId) != null) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 위시리스트에 등록된 공연·구역입니다.");
        }

        try {
            wishlistDAO.insertWishlist(memberId, performanceId, sectorId, priority);
        } catch (SQLException ex) {
            if (ex.getErrorCode() == ORA_PARENT_KEY_NOT_FOUND) {
                throw new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 공연 또는 구역입니다.");
            }
            if (ex.getErrorCode() == ORA_UNIQUE_VIOLATED) {
                throw new ApiException(ErrorCode.CONFLICT, "이미 위시리스트에 등록된 공연·구역입니다.");
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

    /**
     * 순서 일괄 변경. 화면에 보이는 순서대로 ID 전체를 받아 1, 2, 3... 으로 다시 매긴다.
     * 한 트랜잭션이라 중간에 실패하면 전부 롤백된다.
     */
    public List<WishlistResponseDTO> reorder(Long memberId, List<Long> wishlistIds) throws Exception {
        if (wishlistIds == null || wishlistIds.isEmpty() || wishlistIds.contains(null)
                || new HashSet<>(wishlistIds).size() != wishlistIds.size()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "위시리스트 순서가 올바르지 않습니다.");
        }
        // 트랜잭션 밖에서 조회(단독 실행용 메서드와 참여용 메서드를 섞지 않는다)
        Set<Long> currentIds = wishlistDAO.selectWishlistByMemberId(memberId).stream()
                .map(item -> item.getWishlist().getWishlistId())
                .collect(Collectors.toSet());
        if (!currentIds.equals(new HashSet<>(wishlistIds))) {
            throw new ApiException(ErrorCode.CONFLICT, "위시리스트가 변경되었습니다. 새로고침 후 다시 시도해주세요.");
        }

        JDBCUtil tx = new JDBCUtil();
        try {
            for (int i = 0; i < wishlistIds.size(); i++) {
                if (wishlistDAO.updatePriority(tx, wishlistIds.get(i), memberId, i + 1) == 0) {
                    throw new ApiException(ErrorCode.NOT_FOUND, "위시리스트 항목을 찾을 수 없습니다.");
                }
            }
            tx.commit();
        } catch (Exception e) {
            try { tx.rollback(); } catch (Exception ignore) { } // 연결 획득 전 실패면 rollback 자체가 실패할 수 있다
            throw e;
        } finally {
            tx.close();
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

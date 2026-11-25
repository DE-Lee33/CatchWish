package model.dao;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Repository;

import model.dto.WishlistResponseDTO;
import model.mapper.WishlistMapper;

@Repository
public class WishlistDAO {

    private final WishlistMapper wishlistMapper;
    
    public WishlistDAO(WishlistMapper wishlistMapper) {
        this.wishlistMapper = wishlistMapper;
    }
    
    // 1. 위시리스트 등록
    public int insertWishlist(Long memberId, Long performanceId,
                              Long sectorId, Integer priority) throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            String sql = "INSERT INTO WISHLIST "
                       + "(WISHLIST_ID, MEMBER_ID, PERFORMANCE_ID, "
                       + "SECTOR_ID, PRIORITY, CREATED_AT) "
                       + "VALUES (SEQ_WISHLIST.NEXTVAL, ?, ?, ?, ?, SYSDATE)";

            jdbcUtil.setSqlAndParameters(sql,
                new Object[] { memberId, performanceId, sectorId, priority });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            try { jdbcUtil.rollback(); } catch (SQLException ignore) { }   
            throw ex;

        } finally {
            jdbcUtil.close();
        }
    }


    // 2. 회원별 위시리스트 조회 (MyBatis 이용)
    public List<WishlistResponseDTO> selectWishlistByMemberId(Long memberId)
            throws Exception {
        return wishlistMapper.selectWishlistByMemberId(memberId);
    }


    // 3. 위시리스트 삭제
    public int deleteWishlist(Long wishlistId, Long memberId)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            String sql = "DELETE FROM WISHLIST "
                       + "WHERE WISHLIST_ID = ? "
                       + "AND MEMBER_ID = ?";

            jdbcUtil.setSqlAndParameters(sql,
                new Object[] { wishlistId, memberId });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            try { jdbcUtil.rollback(); } catch (SQLException ignore) { } 
            throw ex;

        } finally {
            jdbcUtil.close();
        }
    }


    // 4. 위시리스트 우선순위 변경
    public int updatePriority(Long wishlistId, Long memberId, int priority)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            String sql = "UPDATE WISHLIST "
                       + "SET PRIORITY = ? "
                       + "WHERE WISHLIST_ID = ? "
                       + "AND MEMBER_ID = ?";

            jdbcUtil.setSqlAndParameters(sql,
                new Object[] { priority, wishlistId, memberId });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            try { jdbcUtil.rollback(); } catch (SQLException ignore) { }  
            throw ex;

        } finally {
            jdbcUtil.close();
        }
    }
}
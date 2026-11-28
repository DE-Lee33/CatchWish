package model.dao;

import java.sql.SQLException;
import java.util.List;
import java.sql.ResultSet;

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
    
    /** 구역이 해당 공연의 구역인지 확인 
    * @throws Exception
    * */
    public boolean existsSectorInPerformance(Long sectorId, Long performanceId) throws Exception {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            jdbcUtil.setSqlAndParameters(
                    "SELECT COUNT(*) FROM SECTOR WHERE SECTOR_ID = ? AND PERFORMANCE_ID = ?",
                    new Object[] { sectorId, performanceId });
            ResultSet rs = jdbcUtil.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            jdbcUtil.close();
        }
    }

    /** 트랜잭션 참여용: 전달받은 JDBCUtil을 쓰고 commit/close는 하지 않는다 (Manager가 관리) */
    public int updatePriority(JDBCUtil tx, Long wishlistId, Long memberId, int priority) throws Exception {
        tx.setSqlAndParameters(
                "UPDATE WISHLIST SET PRIORITY = ? WHERE WISHLIST_ID = ? AND MEMBER_ID = ?",
                new Object[] { priority, wishlistId, memberId });
        return tx.executeUpdate();
    }
}

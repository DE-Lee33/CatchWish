
package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import model.domain.Wishlist;
import model.dto.WishlistResponseDTO;

public class WishlistDAO {

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
            jdbcUtil.rollback();
            throw ex;

        } finally {
            jdbcUtil.close();
        }
    }


    // 2. 회원별 위시리스트 조회
    public List<WishlistResponseDTO> selectWishlistByMemberId(Long memberId)
            throws SQLException {

        JDBCUtil jdbcUtil = new JDBCUtil();
        List<WishlistResponseDTO> wishlistList = new ArrayList<>();

        try {
            String sql = "SELECT W.WISHLIST_ID, W.MEMBER_ID, "
                       + "W.PERFORMANCE_ID, W.SECTOR_ID, W.PRIORITY, "
                       + "W.CREATED_AT, P.PERFORMANCE_NAME, "
                       + "P.SHOW_AT, S.SECTOR_NAME "
                       + "FROM WISHLIST W "
                       + "JOIN PERFORMANCE P "
                       + "ON W.PERFORMANCE_ID = P.PERFORMANCE_ID "
                       + "LEFT JOIN SECTOR S "
                       + "ON W.SECTOR_ID = S.SECTOR_ID "
                       + "WHERE W.MEMBER_ID = ? "
                       + "ORDER BY W.PRIORITY ASC NULLS LAST, "
                       + "W.CREATED_AT DESC";

            jdbcUtil.setSqlAndParameters(sql,
                new Object[] { memberId });

            ResultSet rs = jdbcUtil.executeQuery();

            if (rs == null) {
                throw new SQLException("위시리스트 조회에 실패했습니다.");
            }

            while (rs.next()) {

                Long wishlistId = rs.getLong("WISHLIST_ID");
                Long resultMemberId = rs.getLong("MEMBER_ID");
                Long performanceId = rs.getLong("PERFORMANCE_ID");

                Long sectorId = rs.getLong("SECTOR_ID");
                if (rs.wasNull()) {
                    sectorId = null;
                }

                int priorityValue = rs.getInt("PRIORITY");
                Integer priority = rs.wasNull() ? null : priorityValue;

                Timestamp createdAt = rs.getTimestamp("CREATED_AT");

                Wishlist wishlist = new Wishlist(
                    wishlistId,
                    resultMemberId,
                    performanceId,
                    sectorId,
                    priority,
                    createdAt
                );

                String performanceTitle =
                    rs.getString("PERFORMANCE_NAME");

                Timestamp performanceDate =
                    rs.getTimestamp("SHOW_AT");

                String sectorName =
                    rs.getString("SECTOR_NAME");

                WishlistResponseDTO dto = new WishlistResponseDTO(
                    wishlist,
                    performanceTitle,
                    performanceDate,
                    sectorName
                );

                wishlistList.add(dto);
            }

            return wishlistList;

        } finally {
            jdbcUtil.close();
        }
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
            jdbcUtil.rollback();
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
            jdbcUtil.rollback();
            throw ex;

        } finally {
            jdbcUtil.close();
        }
    }
}

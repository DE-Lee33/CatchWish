/*
 * [변경 요약] VenueDAO  (표시: [수정] [추가] [삭제])
 * [삭제] private JDBCUtil jdbcUtil 필드, 생성자 (요청 간 Connection 공유 방지)
 * [추가] selectVenueList, findOne: 메서드 안에서 JDBCUtil 지역 생성
 * [수정] executeQueryOrThrow() -> jdbcUtil.executeQuery()
 * [삭제] executeQueryOrThrow (JDBCUtil.executeQuery()가 예외를 던지므로 불필요)
 */
package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import model.dto.VenueResponseDTO;

@Repository
public class VenueDAO {
    private static final String SELECT_COLUMNS =
            "SELECT venue_id, venue_name, venue_address, venue_capacity FROM venue ";
    
    // [삭제] private JDBCUtil jdbcUtil = null;
    
    // [삭제] public VenueDAO() {
    // [삭제]     jdbcUtil = new JDBCUtil();
    // [삭제] }
    
    public List<VenueResponseDTO> selectVenueList() throws SQLException {
        String sql = SELECT_COLUMNS + "ORDER BY venue_name";
        JDBCUtil jdbcUtil = new JDBCUtil();   // [추가]
        jdbcUtil.setSqlAndParameters(sql, null);
        
        List<VenueResponseDTO> venueList = new ArrayList<>();
        
        try {
            ResultSet rs = jdbcUtil.executeQuery();   // [수정] executeQueryOrThrow() -> executeQuery()
            
            while(rs.next()) {
                venueList.add(mapVenue(rs));
            }
            
            return venueList;
        }
        catch (Exception ex) {
            throw toSQLException("공연장 목록 조회 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    }
    
    public VenueResponseDTO selectVenueById(long venueId) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE venue_id = ?";
        
        return findOne(sql, new Object[] { venueId });
    }
    
    private VenueResponseDTO findOne(String sql, Object[] params) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();   // [추가]
        jdbcUtil.setSqlAndParameters(sql, params);
        
        try {
            ResultSet rs = jdbcUtil.executeQuery();   // [수정]
            
            if(rs.next()) {
                return mapVenue(rs);
            }
            
            return null;
        }
        catch (Exception ex) {
            throw toSQLException("공연장 상세 조회 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    }
    
    private VenueResponseDTO mapVenue(ResultSet rs) throws SQLException {
        VenueResponseDTO venue = new VenueResponseDTO();
        
        venue.setVenueId(rs.getLong("venue_id"));
        venue.setVenueName(rs.getString("venue_name"));
        venue.setVenueAddress(rs.getString("venue_address"));
        venue.setVenueCapacity(rs.getInt("venue_capacity"));
    
        return venue;
    }
    
    // [삭제] private ResultSet executeQueryOrThrow() throws SQLException {
    // [삭제]     ResultSet rs = jdbcUtil.executeQuery();
    // [삭제]     if(rs == null) {
    // [삭제]         throw new SQLException("쿼리 실행 실패: " + jdbcUtil.getSql());
    // [삭제]     }
    // [삭제]     return rs;
    // [삭제] }
    
    private SQLException toSQLException(String message, Exception ex) {
        if(ex instanceof SQLException) {
            return (SQLException) ex;
        }
        
        return new SQLException(message, ex);
    }
}
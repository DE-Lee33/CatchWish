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
    
    public List<VenueResponseDTO> selectVenueList() throws SQLException {
        String sql = SELECT_COLUMNS + "ORDER BY venue_name";
        JDBCUtil jdbcUtil = new JDBCUtil(); 
        jdbcUtil.setSqlAndParameters(sql, null);
        
        List<VenueResponseDTO> venueList = new ArrayList<>();
        
        try {
            ResultSet rs = jdbcUtil.executeQuery();  
            
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
        JDBCUtil jdbcUtil = new JDBCUtil();  
        jdbcUtil.setSqlAndParameters(sql, params);
        
        try {
            ResultSet rs = jdbcUtil.executeQuery(); 
            
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
    
    // 트랜잭션 참여용: 템플릿 없이 직접 그린 공연용 공연장
    public static long createVenue(JDBCUtil jdbcUtil, String venueName, String venueAddress, int capacity) throws Exception {
        String sql = "INSERT INTO venue (venue_id, venue_name, venue_address, venue_capacity) VALUES (?, ?, ?, ?)";
        long venueId = nextSequence(jdbcUtil, "SEQ_VENUE");   // 시퀀스 이름 확인 필요
        jdbcUtil.setSqlAndParameters(sql, new Object[] { venueId, venueName, venueAddress, capacity });
        jdbcUtil.executeUpdate();
        return venueId;
    }
    
    /** 시퀀스 다음 값 조회 (PerformanceDAO의 것과 동일) */
    private static long nextSequence(JDBCUtil jdbcUtil, String sequenceName) throws Exception {
        jdbcUtil.setSqlAndParameters("SELECT " + sequenceName + ".NEXTVAL FROM DUAL", null);
        ResultSet rs = jdbcUtil.executeQuery();
        if (!rs.next()) {
            throw new SQLException(sequenceName + " 값을 가져오지 못했습니다.");
        }
        return rs.getLong(1);
    }
    
    private SQLException toSQLException(String message, Exception ex) {
        if(ex instanceof SQLException) {
            return (SQLException) ex;
        }
        
        return new SQLException(message, ex);
    }
}
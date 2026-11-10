package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.dto.SeatLayoutResponseDTO;

public class SeatLayoutDAO {
    private JDBCUtil jdbcUtil = null;
    
    public SeatLayoutDAO() {
        jdbcUtil = new JDBCUtil();
    }
    
    public List<SeatLayoutResponseDTO> selectLayoutWithSectors(long seatLayoutId) throws SQLException {
        String sql = "SELECT sl.seat_layout_id, sl.venue_id, sl.seat_layout_name, sl.is_template, s.sector_id, s.sector_name "
                   + "FROM seat_layout sl JOIN sector s ON sl.seat_layout_id = s.seat_layout_id "
                   + "WHERE s.seat_layout_id = ?";
        
        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId });
        
        List<SeatLayoutResponseDTO> list = new ArrayList<>();
        
        try {
            ResultSet rs = executeQueryOrThrow();
            
            while(rs.next()) {
                SeatLayoutResponseDTO dto = new SeatLayoutResponseDTO();
                
                dto.setSeatLayoutId(rs.getLong("seat_layout_id"));
                dto.setVenueId(rs.getLong("venue_id"));
                dto.setSeatLayoutName(rs.getString("seat_layout_name"));
                dto.setIsTemplate(rs.getString("is_template"));
                dto.setSectorId(rs.getLong("sector_id"));
                dto.setSectorName(rs.getString("sector_name"));
                
                list.add(dto);
            }
            
            return list;
        }
        catch(Exception ex) {
            throw toSQLException("좌석 템플릿 구조 조회 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    }
    
    public int insertSeat(long seatLayoutId, long sectorId, int rowNum, int columnNum, String seatNumber) throws SQLException {
        String sql = "INSERT INTO seat(seat_id, seat_layout_id, sector_id, row_num, column_num, seat_number, seat_status) "
                   + "VALUES (SEQ_SEAT.NEXTVAL, ?, ?, ?, ?, ?, 'A')";
        
        return executeChange(sql, new Object[] { seatLayoutId, sectorId, rowNum, columnNum, seatNumber });
    }
    
    public int updateSeat(long sectorId, int rowNum, int columnNum, String seatNumber, long seatId) throws SQLException {
        String sql = "UPDATE seat SET sector_id = ?, row_num = ?, column_num = ?, seat_number = ? WHERE seat_id = ?";
        
        return executeChange(sql, new Object[] { sectorId, rowNum, columnNum, seatNumber, seatId });
    }
    
    public int insertSector(long seatLayoutId, String sectorName) throws SQLException {
        String sql = "INSERT INTO sector (sector_id, seat_layout_id, sector_name) VALUES (SEQ_SECTOR.NEXTVAL, ?, ?)";
        
        return executeChange(sql, new Object[] { seatLayoutId, sectorName });
    }
    
    public int updateSeatStatusToBooked(long seatId) throws SQLException {
        String sql = "UPDATE seat SET seat_status = 'B' WHERE seat_id = ?";
        
        return executeChange(sql, new Object[] { seatId });
    }
    
    public int updateSeatStatusToAvailable(long bookingId) throws SQLException {
        String sql = "UPDATE seat SET seat_status = 'A' "
                   + "WHERE seat_id IN (SELECT seat_id FROM booking_seat WHERE booking_id = ?) "
                   + "AND seat_status IN ('H', 'C')";
        
        return executeChange(sql, new Object[] { bookingId });
    }
    
    private int executeChange(String sql, Object[] params) throws SQLException {
        jdbcUtil.setSqlAndParameters(sql, params);
        
        try {
            int result = jdbcUtil.executeUpdate();
            
            jdbcUtil.commit();
            
            return result;
        } 
        catch (Exception ex) {
            rollbackQuietly();
            
            throw toSQLException("좌석/구역 변경 처리 실패", ex);
        } 
        finally {
            jdbcUtil.close();
        }
    }
    
    private ResultSet executeQueryOrThrow() throws SQLException {
        ResultSet rs = jdbcUtil.executeQuery();
        
        if (rs == null) {
            throw new SQLException("쿼리 실행 실패: " + jdbcUtil.getSql());
        }
        
        return rs;
    }
    
    private void rollbackQuietly() {
        try {
            jdbcUtil.rollback();
        } 
        catch (Exception ignore) {

        }
    }
    
    private SQLException toSQLException(String message, Exception ex) {
        if (ex instanceof SQLException) {
            return (SQLException) ex;
        }
        
        return new SQLException(message, ex);
    }
}

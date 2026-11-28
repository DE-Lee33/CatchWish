package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import model.dto.SeatLayoutResponseDTO;
import model.domain.enums.SeatStatus;

@Repository
public class SeatLayoutDAO {
    public List<SeatLayoutResponseDTO> selectLayoutWithSectors(long seatLayoutId) throws SQLException {
        String sql = "SELECT sl.seat_layout_id, sl.venue_id, sl.seat_layout_name, sl.is_template, s.sector_id, s.sector_name "
                   + "FROM seat_layout sl JOIN sector s ON sl.seat_layout_id = s.seat_layout_id "
                   + "WHERE s.seat_layout_id = ?";
        
        JDBCUtil jdbcUtil = new JDBCUtil();  
        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId });
        
        List<SeatLayoutResponseDTO> list = new ArrayList<>();
        
        try {
            ResultSet rs = jdbcUtil.executeQuery();  
            
            while(rs.next()) {
                SeatLayoutResponseDTO dto = new SeatLayoutResponseDTO();
                
                dto.setSeatLayoutId(rs.getLong("seat_layout_id"));
                dto.setVenueId(rs.getLong("venue_id"));
                dto.setSeatLayoutName(rs.getString("seat_layout_name"));
                dto.setIsTemplate("Y".equals(rs.getString("is_template")));                dto.setSectorId(rs.getLong("sector_id"));
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
    
    // 단독용: 참여용을 호출하고 commit
    public int insertSeat(long seatLayoutId, long sectorId, String rowNum, int columnNum, String seatNumber) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = insertSeat(jdbcUtil, seatLayoutId, sectorId, rowNum, columnNum, seatNumber);
            jdbcUtil.commit();
            return result;
        }
        catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("좌석/구역 변경 처리 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    }
    
    // 트랜잭션 참여용 (commit/rollback/close는 호출한 쪽에서)
    public int insertSeat(JDBCUtil jdbcUtil, long seatLayoutId, long sectorId, String rowNum, int columnNum, String seatNumber) throws Exception {
        String sql = "INSERT INTO seat(seat_id, seat_layout_id, sector_id, row_num, column_num, seat_number, seat_status) "
                   + "VALUES (SEQ_SEAT.NEXTVAL, ?, ?, ?, ?, ?, ?)";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId, sectorId, rowNum, columnNum, seatNumber, SeatStatus.AVAILABLE.getCode() });
        
        return jdbcUtil.executeUpdate();
    }
    
    public int updateSeat(long sectorId, String rowNum, int columnNum, String seatNumber, long seatId) throws SQLException {
        String sql = "UPDATE seat SET sector_id = ?, row_num = ?, column_num = ?, seat_number = ? WHERE seat_id = ?";
        
        return executeChange(sql, new Object[] { sectorId, rowNum, columnNum, seatNumber, seatId });
    }
    
    // 단독용
    public int insertSector(long seatLayoutId, String sectorName, long gradeId, long performanceId) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = insertSector(jdbcUtil, seatLayoutId, sectorName, gradeId, performanceId);
            jdbcUtil.commit();
            return result;
        }
        catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("좌석/구역 변경 처리 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    }
    
    // 트랜잭션 참여용
    public int insertSector(JDBCUtil jdbcUtil, long seatLayoutId, String sectorName, long gradeId, long performanceId) throws Exception {
        String sql = "INSERT INTO sector (sector_id, seat_layout_id, sector_name, grade_id, performance_id) VALUES (SEQ_SECTOR.NEXTVAL, ?, ?, ?, ?)";
        
        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId, sectorName, gradeId, performanceId });
        return jdbcUtil.executeUpdate();
    }
    
    // 단독용
    public int updateSeatStatusToBooked(long seatId) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateSeatStatusToBooked(jdbcUtil, seatId);
            jdbcUtil.commit();
            return result;
        }
        catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("좌석/구역 변경 처리 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    } 
    
    // 트랜잭션 참여용. 0이 반환되면 H 상태가 아님 → 호출한 쪽에서 롤백
    public int updateSeatStatusToBooked(JDBCUtil jdbcUtil, long seatId) throws Exception {
        String sql = "UPDATE seat SET seat_status = ? WHERE seat_id = ? AND seat_status = ?";

        jdbcUtil.setSqlAndParameters(sql, new Object[] {
            SeatStatus.BOOKED.getCode(),
            seatId,
            SeatStatus.HELD.getCode()
        });
        return jdbcUtil.executeUpdate();
    }
    
    // 단독용
    public int updateSeatStatusToAvailable(long bookingId) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            int result = updateSeatStatusToAvailable(jdbcUtil, bookingId);
            jdbcUtil.commit();
            return result;
        }
        catch (Exception ex) {
            rollbackQuietly(jdbcUtil);
            throw toSQLException("좌석/구역 변경 처리 실패", ex);
        }
        finally {
            jdbcUtil.close();
        }
    }
    
    // 트랜잭션 참여용
    public int updateSeatStatusToAvailable(JDBCUtil jdbcUtil, long bookingId) throws Exception {
        String sql = "UPDATE seat SET seat_status = ? "
                   + "WHERE seat_id IN (SELECT seat_id FROM booking_seat WHERE booking_id = ?) "
                   + "AND seat_status IN (?, ?)";

        jdbcUtil.setSqlAndParameters(sql, new Object[] {
            SeatStatus.AVAILABLE.getCode(),
            bookingId,
            SeatStatus.HELD.getCode(),
            SeatStatus.CANCEL_PENDING.getCode()
        });
        return jdbcUtil.executeUpdate();
    }
    
     // 트랜잭션 참여용 (commit/rollback/close는 호출한 쪽에서)
     // M9 등록 시 항상 새 레이아웃(IS_TEMPLATE='N'). 템플릿을 불러왔으면 sourceLayoutId에 원본 id
     public long createSeatLayout(JDBCUtil jdbcUtil, String seatLayoutName, long venueId, Long sourceLayoutId, boolean isTemplate) throws Exception {
         String sql = "INSERT INTO seat_layout (seat_layout_id, seat_layout_name, is_template, venue_id, source_layout_id) "
                 + "VALUES (?, ?, ?, ?, ?)";
         long seatLayoutId = nextSequence(jdbcUtil, "SEQ_SEAT_LAYOUT");
         
         jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId, seatLayoutName, isTemplate ? "Y" : "N", venueId, sourceLayoutId });
         jdbcUtil.executeUpdate();
         
         return seatLayoutId;
     }
    
     // 트랜잭션 참여용: 생성된 sector_id를 반환 (좌석 insert에 필요)
     public long createSector(JDBCUtil jdbcUtil, long seatLayoutId, String sectorName, long gradeId, long performanceId) throws Exception {
         String sql = "INSERT INTO sector (sector_id, seat_layout_id, sector_name, grade_id, performance_id) VALUES (?, ?, ?, ?, ?)";
         long sectorId = nextSequence(jdbcUtil, "SEQ_SECTOR");
         jdbcUtil.setSqlAndParameters(sql, new Object[] { sectorId, seatLayoutId, sectorName, gradeId, performanceId });
         jdbcUtil.executeUpdate();
         return sectorId;
     }
    
     /** 시퀀스 다음 값 조회 (PerformanceDAO의 것과 동일) */
     private long nextSequence(JDBCUtil jdbcUtil, String sequenceName) throws Exception {
         jdbcUtil.setSqlAndParameters("SELECT " + sequenceName + ".NEXTVAL FROM DUAL", null);
         ResultSet rs = jdbcUtil.executeQuery();
         if (!rs.next()) {
             throw new SQLException(sequenceName + " 값을 가져오지 못했습니다.");
         }
         return rs.getLong(1);
     }
    
    private int executeChange(String sql, Object[] params) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();  
        jdbcUtil.setSqlAndParameters(sql, params);
        
        try {
            int result = jdbcUtil.executeUpdate();
            
            jdbcUtil.commit();
            
            return result;
        } 
        catch (Exception ex) {
            rollbackQuietly(jdbcUtil); 
            
            throw toSQLException("좌석/구역 변경 처리 실패", ex);
        } 
        finally {
            jdbcUtil.close();
        }
    }
    
    private void rollbackQuietly(JDBCUtil jdbcUtil) {  
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
/*
 * [변경 요약] SeatLayoutDAO  (표시: [수정] [추가] [삭제])
 * [삭제] private JDBCUtil jdbcUtil 필드, 생성자 (요청 간 Connection 공유 방지)
 * [수정] selectLayoutWithSectors, executeChange: 메서드 안에서 JDBCUtil 지역 생성
 * [추가] 트랜잭션 참여용 오버로드 4개: insertSeat, insertSector, updateSeatStatusToBooked, updateSeatStatusToAvailable
 * [수정] 위 4개의 단독용 메서드는 참여용을 호출하고 commit
 * [수정] rollbackQuietly: JDBCUtil을 파라미터로 받음
 * [삭제] executeQueryOrThrow (JDBCUtil.executeQuery()가 예외를 던지므로 불필요)
 * 변경 없음: updateSeat (executeChange 사용)
 */
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
    // [삭제] private JDBCUtil jdbcUtil = null;
    
    // [삭제] public SeatLayoutDAO() {
    // [삭제]     jdbcUtil = new JDBCUtil();
    // [삭제] }
    
    public List<SeatLayoutResponseDTO> selectLayoutWithSectors(long seatLayoutId) throws SQLException {
        String sql = "SELECT sl.seat_layout_id, sl.venue_id, sl.seat_layout_name, sl.is_template, s.sector_id, s.sector_name "
                   + "FROM seat_layout sl JOIN sector s ON sl.seat_layout_id = s.seat_layout_id "
                   + "WHERE s.seat_layout_id = ?";
        
        JDBCUtil jdbcUtil = new JDBCUtil();   // [추가]
        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId });
        
        List<SeatLayoutResponseDTO> list = new ArrayList<>();
        
        try {
            ResultSet rs = jdbcUtil.executeQuery();   // [수정] executeQueryOrThrow() -> executeQuery()
            
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
    
    // [수정] 단독용: 참여용을 호출하고 commit
    public int insertSeat(long seatLayoutId, long sectorId, int rowNum, int columnNum, String seatNumber) throws SQLException {
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
    
    // [추가] 트랜잭션 참여용 (commit/rollback/close는 호출한 쪽에서)
    public int insertSeat(JDBCUtil jdbcUtil, long seatLayoutId, long sectorId, int rowNum, int columnNum, String seatNumber) throws Exception {
        String sql = "INSERT INTO seat(seat_id, seat_layout_id, sector_id, row_num, column_num, seat_number, seat_status) "
                   + "VALUES (SEQ_SEAT.NEXTVAL, ?, ?, ?, ?, ?, ?)";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId, sectorId, rowNum, columnNum, seatNumber, SeatStatus.AVAILABLE.getCode() });
        
        return jdbcUtil.executeUpdate();
    }
    
    public int updateSeat(long sectorId, int rowNum, int columnNum, String seatNumber, long seatId) throws SQLException {
        String sql = "UPDATE seat SET sector_id = ?, row_num = ?, column_num = ?, seat_number = ? WHERE seat_id = ?";
        
        return executeChange(sql, new Object[] { sectorId, rowNum, columnNum, seatNumber, seatId });
    }
    
    // [수정] 단독용
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
    
    // [추가] 트랜잭션 참여용
    public int insertSector(JDBCUtil jdbcUtil, long seatLayoutId, String sectorName, long gradeId, long performanceId) throws Exception {
        String sql = "INSERT INTO sector (sector_id, seat_layout_id, sector_name, grade_id, performance_id) VALUES (SEQ_SECTOR.NEXTVAL, ?, ?, ?, ?)";
        
        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId, sectorName, gradeId, performanceId });
        return jdbcUtil.executeUpdate();
    }
    
    // [수정] 단독용
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
    
    // [추가] 트랜잭션 참여용. 0이 반환되면 H 상태가 아님 → 호출한 쪽에서 롤백
    public int updateSeatStatusToBooked(JDBCUtil jdbcUtil, long seatId) throws Exception {
        String sql = "UPDATE seat SET seat_status = ? WHERE seat_id = ? AND seat_status = ?";

        jdbcUtil.setSqlAndParameters(sql, new Object[] {
            SeatStatus.BOOKED.getCode(),
            seatId,
            SeatStatus.HELD.getCode()
        });
        return jdbcUtil.executeUpdate();
    }
    
    // [수정] 단독용
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
    
    // [추가] 트랜잭션 참여용
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
    
    private int executeChange(String sql, Object[] params) throws SQLException {
        JDBCUtil jdbcUtil = new JDBCUtil();   // [추가]
        jdbcUtil.setSqlAndParameters(sql, params);
        
        try {
            int result = jdbcUtil.executeUpdate();
            
            jdbcUtil.commit();
            
            return result;
        } 
        catch (Exception ex) {
            rollbackQuietly(jdbcUtil);   // [수정]
            
            throw toSQLException("좌석/구역 변경 처리 실패", ex);
        } 
        finally {
            jdbcUtil.close();
        }
    }
    
    // [삭제] private ResultSet executeQueryOrThrow() throws SQLException {
    // [삭제]     ResultSet rs = jdbcUtil.executeQuery();
    // [삭제]     if (rs == null) {
    // [삭제]         throw new SQLException("쿼리 실행 실패: " + jdbcUtil.getSql());
    // [삭제]     }
    // [삭제]     return rs;
    // [삭제] }
    
    private void rollbackQuietly(JDBCUtil jdbcUtil) {   // [수정] 파라미터 추가
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
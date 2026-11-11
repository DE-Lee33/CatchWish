package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.dto.SeatLayoutResponseDTO;

/**
 * SeatLayoutDAO 테스트 클래스
 * 팀 공용 DB를 쓰므로, 테스트에서 만든 데이터는 끝날 때 반드시 지운다.
 */
public class SeatLayoutDAOTest {

    private SeatLayoutDAO seatLayoutDao;
    
    // 테스트 간 데이터 격리를 위한 ID 변수들
    private long createdSeatLayoutId = 0;
    private long createdSectorId = 0;
    private long createdSeatId = 0;

    @Before
    public void setUp() {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        seatLayoutDao = new SeatLayoutDAO();
    }

    @After
    public void tearDown() throws Exception {
        // 생성된 테스트 데이터가 있다면 역순(Seat -> Sector -> Layout)으로 안전하게 삭제
        if (createdSeatLayoutId == 0 && createdSectorId == 0 && createdSeatId == 0) {
            return;
        }
        
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            if (createdSeatId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT WHERE SEAT_ID = ?", new Object[] { createdSeatId });
                jdbcUtil.executeUpdate();
            }
            if (createdSectorId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SECTOR WHERE SECTOR_ID = ?", new Object[] { createdSectorId });
                jdbcUtil.executeUpdate();
            }
            if (createdSeatLayoutId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_ID = ?", new Object[] { createdSeatLayoutId });
                jdbcUtil.executeUpdate();
            }
            jdbcUtil.commit();
        } catch (Exception e) {
            jdbcUtil.rollback();
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }

    @Test
    public void testSelectLayoutWithSectors() throws Exception {
        createdSeatLayoutId = insertTestSeatLayout("테스트레이아웃");
        createdSectorId = insertTestSector(createdSeatLayoutId, "테스트구역", 1L, 1L);

        List<SeatLayoutResponseDTO> layoutList = seatLayoutDao.selectLayoutWithSectors(createdSeatLayoutId);

        assertNotNull("레이아웃 및 구역 목록이 null이 아니어야 합니다.", layoutList);
        assertFalse("조회된 구역이 존재해야 합니다.", layoutList.isEmpty());

        for (SeatLayoutResponseDTO dto : layoutList) {
            assertEquals("조회된 레이아웃 ID가 일치해야 합니다.", createdSeatLayoutId, dto.getSeatLayoutId());
            assertEquals("조회된 구역 ID가 일치해야 합니다.", createdSectorId, dto.getSectorId());
        }
    }

    @Test
    public void testInsertSectorAndSeat() throws Exception {
        createdSeatLayoutId = insertTestSeatLayout("테스트레이아웃_인서트");
        long testGradeId = 1L;
        long testPerformanceId = 1L;

        int sectorResult = seatLayoutDao.insertSector(createdSeatLayoutId, "A구역", testGradeId, testPerformanceId);
        assertTrue("구역 추가가 성공해야 합니다.", sectorResult > 0);

        createdSectorId = findLatestSectorId(createdSeatLayoutId);
        assertTrue(createdSectorId > 0);

        int seatResult = seatLayoutDao.insertSeat(createdSeatLayoutId, createdSectorId, 1, 1, "A-1");
        assertTrue("좌석 등록이 성공해야 합니다.", seatResult > 0);
        
        createdSeatId = findLatestSeatId(createdSeatLayoutId);
        assertTrue("생성된 좌석 ID가 존재해야 합니다.", createdSeatId > 0);
    }

    @Test
    public void testUpdateSeatStatus() throws Exception {
        createdSeatLayoutId = insertTestSeatLayout("테스트레이아웃_상태변경");
        createdSectorId = insertTestSector(createdSeatLayoutId, "VIP구역", 1L, 1L);
        
        // 테스트용 좌석 생성 (선점 상태 'H'로 생성)
        createdSeatId = insertTestSeat(createdSeatLayoutId, createdSectorId, 1, 1, "V-1", "H");

        // 선점된 좌석을 예매완료('B')로 변경 테스트
        int bookResult = seatLayoutDao.updateSeatStatusToBooked(createdSeatId);
        assertEquals("선점 상태인 좌석만 예매 완료로 변경되어야 합니다.", 1, bookResult);
    }

    // --- 테스트 데이터 세팅용 헬퍼 메서드들 (throws Exception 선언 및 ResultSet 타입 적용) ---
    
    private long insertTestSeatLayout(String name) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO SEAT_LAYOUT (SEAT_LAYOUT_ID, VENUE_ID, SEAT_LAYOUT_NAME, IS_TEMPLATE) "
                       + "VALUES (SEQ_SEAT_LAYOUT.NEXTVAL, 1, ?, 'Y')";
            jdbc.setSqlAndParameters(sql, new Object[] { name });
            jdbc.executeUpdate();
            jdbc.commit();
            
            jdbc.setSqlAndParameters("SELECT SEQ_SEAT_LAYOUT.CURRVAL FROM DUAL", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestSector(long layoutId, String sectorName, long gradeId, long perfId) throws Exception {
        seatLayoutDao.insertSector(layoutId, sectorName, gradeId, perfId);
        return findLatestSectorId(layoutId);
    }

    private long findLatestSectorId(long layoutId) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT MAX(SECTOR_ID) FROM SECTOR WHERE SEAT_LAYOUT_ID = ?", new Object[] { layoutId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }
    
    private long findLatestSeatId(long layoutId) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters(
                "SELECT MAX(SEAT_ID) FROM SEAT WHERE SEAT_LAYOUT_ID = ?",
                new Object[] { layoutId }
            );
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } finally {
            jdbc.close();
        }
        return -1;
    }

    private long insertTestSeat(long layoutId, long sectorId, int row, int col, String seatNum, String status) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            String sql = "INSERT INTO SEAT (SEAT_ID, SEAT_LAYOUT_ID, SECTOR_ID, ROW_NUM, COLUMN_NUM, SEAT_NUMBER, SEAT_STATUS) "
                       + "VALUES (SEQ_SEAT.NEXTVAL, ?, ?, ?, ?, ?, ?)";
            jdbc.setSqlAndParameters(sql, new Object[] { layoutId, sectorId, row, col, seatNum, status });
            jdbc.executeUpdate();
            jdbc.commit();

            jdbc.setSqlAndParameters("SELECT MAX(SEAT_ID) FROM SEAT WHERE SEAT_LAYOUT_ID = ?", new Object[] { layoutId });
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } finally {
            jdbc.close();
        }
        return -1;
    }
}
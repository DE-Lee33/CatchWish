package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;
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
    private long existingGradeId = 0;
    private long existingPerformanceId = 0;

    @Before
    public void setUp() throws Exception {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        seatLayoutDao = new SeatLayoutDAO();
        System.out.println("\n[SETUP] SeatLayoutDAO 생성 완료, db.properties 확인됨");

        // SEAT_GRADE의 같은 행에서 GRADE_ID, PERFORMANCE_ID를 함께 조회 (FK 충족)
        long[] gp = findAnyGradeAndPerformance();
        assumeTrue("SEAT_GRADE 데이터가 필요합니다.", gp != null);
        existingGradeId = gp[0];
        existingPerformanceId = gp[1];
        System.out.println("[SETUP] 기존 GRADE_ID=" + existingGradeId
                + ", PERFORMANCE_ID=" + existingPerformanceId + " 사용");
    }

    @After
    public void tearDown() throws Exception {
        // 생성된 테스트 데이터가 있다면 역순(Seat -> Sector -> Layout)으로 안전하게 삭제
        if (createdSeatLayoutId == 0 && createdSectorId == 0 && createdSeatId == 0) {
            System.out.println("[TEARDOWN] 정리할 데이터 없음");
            return;
        }

        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            if (createdSeatId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT WHERE SEAT_ID = ?", new Object[] { createdSeatId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] SEAT 삭제: " + n + "건");
            }
            if (createdSectorId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SECTOR WHERE SECTOR_ID = ?", new Object[] { createdSectorId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] SECTOR 삭제: " + n + "건");
            }
            if (createdSeatLayoutId > 0) {
                jdbcUtil.setSqlAndParameters("DELETE FROM SEAT_LAYOUT WHERE SEAT_LAYOUT_ID = ?", new Object[] { createdSeatLayoutId });
                int n = jdbcUtil.executeUpdate();
                System.out.println("[TEARDOWN] SEAT_LAYOUT 삭제: " + n + "건");
            }
            jdbcUtil.commit();
            System.out.println("[TEARDOWN] commit 완료");
        } catch (Exception e) {
            try { jdbcUtil.rollback(); } catch (Exception ignore) { }
            System.out.println("[TEARDOWN] 실패, rollback: " + e.getMessage());
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }

    @Test
    public void testSelectLayoutWithSectors() throws Exception {
        System.out.println("\n[TEST] selectLayoutWithSectors 시작");
        createdSeatLayoutId = insertTestSeatLayout("테스트레이아웃");
        System.out.println("[INFO] 테스트 SEAT_LAYOUT 생성: " + createdSeatLayoutId);
        createdSectorId = insertTestSector(createdSeatLayoutId, "테스트구역", existingGradeId, existingPerformanceId);
        System.out.println("[INFO] 테스트 SECTOR 생성: " + createdSectorId);

        List<SeatLayoutResponseDTO> layoutList = seatLayoutDao.selectLayoutWithSectors(createdSeatLayoutId);

        assertNotNull("레이아웃 및 구역 목록이 null이 아니어야 합니다.", layoutList);
        assertFalse("조회된 구역이 존재해야 합니다.", layoutList.isEmpty());
        System.out.println("[PASS] 조회 결과 " + layoutList.size() + "건");

        for (SeatLayoutResponseDTO dto : layoutList) {
            System.out.println("  레이아웃ID: " + dto.getSeatLayoutId()
                    + ", 이름: " + dto.getSeatLayoutName()
                    + ", 구역ID: " + dto.getSectorId()
                    + ", 구역명: " + dto.getSectorName());
            assertEquals("조회된 레이아웃 ID가 일치해야 합니다.", createdSeatLayoutId, dto.getSeatLayoutId());
            assertEquals("조회된 구역 ID가 일치해야 합니다.", createdSectorId, dto.getSectorId());
        }
        System.out.println("[PASS] 레이아웃 ID, 구역 ID 모두 일치");
    }

    @Test
    public void testInsertSectorAndSeat() throws Exception {
        System.out.println("\n[TEST] insertSector / insertSeat 시작");
        createdSeatLayoutId = insertTestSeatLayout("테스트레이아웃_인서트");
        System.out.println("[INFO] 테스트 SEAT_LAYOUT 생성: " + createdSeatLayoutId);

        int sectorResult = seatLayoutDao.insertSector(createdSeatLayoutId, "A구역", existingGradeId, existingPerformanceId);
        System.out.println("[INFO] insertSector 결과: " + sectorResult);
        assertTrue("구역 추가가 성공해야 합니다.", sectorResult > 0);
        System.out.println("[PASS] 구역 추가 성공");

        createdSectorId = findLatestSectorId(createdSeatLayoutId);
        assertTrue(createdSectorId > 0);
        System.out.println("[PASS] 생성된 SECTOR_ID: " + createdSectorId);

        int seatResult = seatLayoutDao.insertSeat(createdSeatLayoutId, createdSectorId, 1, 1, "A-1");
        System.out.println("[INFO] insertSeat 결과: " + seatResult);
        assertTrue("좌석 등록이 성공해야 합니다.", seatResult > 0);
        System.out.println("[PASS] 좌석 등록 성공");

        createdSeatId = findLatestSeatId(createdSeatLayoutId);
        assertTrue("생성된 좌석 ID가 존재해야 합니다.", createdSeatId > 0);
        System.out.println("[PASS] 생성된 SEAT_ID: " + createdSeatId);
    }

    @Test
    public void testUpdateSeatStatus() throws Exception {
        System.out.println("\n[TEST] updateSeatStatusToBooked 시작");
        createdSeatLayoutId = insertTestSeatLayout("테스트레이아웃_상태변경");
        createdSectorId = insertTestSector(createdSeatLayoutId, "VIP구역", existingGradeId, existingPerformanceId);
        System.out.println("[INFO] 테스트 SEAT_LAYOUT=" + createdSeatLayoutId + ", SECTOR=" + createdSectorId + " 생성");

        // 테스트용 좌석 생성 (선점 상태 'H'로 생성)
        createdSeatId = insertTestSeat(createdSeatLayoutId, createdSectorId, "A", 1, "V-1", "H");
        System.out.println("[INFO] 테스트 SEAT 생성 (상태 H): " + createdSeatId);

        // 선점된 좌석을 예매완료('B')로 변경 테스트
        int bookResult = seatLayoutDao.updateSeatStatusToBooked(createdSeatId);
        System.out.println("[INFO] 상태 변경 결과: " + bookResult);
        assertEquals("선점 상태인 좌석만 예매 완료로 변경되어야 합니다.", 1, bookResult);
        System.out.println("[PASS] H → B 변경 성공");

        int again = seatLayoutDao.updateSeatStatusToBooked(createdSeatId);
        System.out.println("[INFO] 재호출 결과: " + again);
        assertEquals("이미 예매 완료된 좌석은 다시 변경되면 안 됩니다.", 0, again);
        System.out.println("[PASS] 이미 B 상태인 좌석은 0건 변경");
    }

    // --- 테스트 데이터 세팅용 헬퍼 메서드들 ---

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

    private long insertTestSeat(long layoutId, long sectorId, String row, int col, String seatNum, String status) throws Exception {
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

    private long[] findAnyGradeAndPerformance() throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters(
                "SELECT GRADE_ID, PERFORMANCE_ID FROM SEAT_GRADE WHERE ROWNUM = 1", null);
            ResultSet rs = jdbc.executeQuery();
            if (rs.next()) return new long[] { rs.getLong(1), rs.getLong(2) };
        } finally {
            jdbc.close();
        }
        return null;
    }
}

package model.dao;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import model.dto.SeatLayoutResponseDTO;

/**
 * SeatLayoutDAO 테스트 클래스
 */
public class SeatLayoutDAOTest {

    private SeatLayoutDAO seatLayoutDao;

    @Before
    public void setUp() {
        seatLayoutDao = new SeatLayoutDAO();
    }

    @Test
    public void testSelectLayoutWithSectors() {
        long testSeatLayoutId = 1L; // 실제 테스트 DB에 존재하는 레이아웃 ID로 변경해주세요.

        try {
            List<SeatLayoutResponseDTO> layoutList = seatLayoutDao.selectLayoutWithSectors(testSeatLayoutId);

            assertNotNull("레이아웃 및 구역 목록이 null이 아니어야 합니다.", layoutList);

            System.out.println("=== 템플릿 구조 조회 결과 (총 " + layoutList.size() + "개 구역) ===");
            for (SeatLayoutResponseDTO dto : layoutList) {
                System.out.println("레이아웃 ID: " + dto.getSeatLayoutId() 
                        + ", 레이아웃 이름: " + dto.getSeatLayoutName() 
                        + ", 구역 ID: " + dto.getSectorId() 
                        + ", 구역 이름: " + dto.getSectorName());

                assertEquals("조회된 레이아웃 ID가 일치해야 합니다.", testSeatLayoutId, dto.getSeatLayoutId());
            }
        } catch (SQLException e) {
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }

    @Test
    public void testInsertSectorAndSeat() {
        long testSeatLayoutId = 1L; // 실제 테스트 DB에 존재하는 레이아웃 ID

        try {
            // 1. 새로운 구역 추가 테스트
            int sectorResult = seatLayoutDao.insertSector(testSeatLayoutId, "A구역");
            assertTrue("구역 추가가 성공해야 합니다.", sectorResult > 0);
            System.out.println("구역 추가 성공!");

            // 2. 좌석 배치 등록 테스트 (임의의 sectorId와 데이터 사용)
            long testSectorId = 1L; // 실제 존재하는 sectorId로 변경 가능
            int seatResult = seatLayoutDao.insertSeat(testSeatLayoutId, testSectorId, 1, 1, "A-1");
            assertTrue("좌석 등록이 성공해야 합니다.", seatResult > 0);
            System.out.println("좌석 등록 성공!");

        } catch (SQLException e) {
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateSeatStatus() {
        long testSeatId = 1L;     // 테스트할 물리 좌석 ID
        long testBookingId = 1L;  // 테스트할 예매 ID

        try {
            // 1. 좌석 상태 예매완료('B')로 변경 테스트
            int bookResult = seatLayoutDao.updateSeatStatusToBooked(testSeatId);
            System.out.println("좌석 예매완료 상태 변경 결과 행 수: " + bookResult);

            // 2. 예매 취소 또는 홀드 만료 시 예매가능('A')으로 변경 테스트
            int cancelResult = seatLayoutDao.updateSeatStatusToAvailable(testBookingId);
            System.out.println("좌석 예매가능 상태 변경 결과 행 수: " + cancelResult);

        } catch (SQLException e) {
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }
}
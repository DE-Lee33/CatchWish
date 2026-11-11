
package model.dao;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import model.domain.Seat;
import model.dto.SectorAvailabilityDTO;

public class SeatDAOTest {

    private SeatDAO seatDAO;

    @Before
    public void setUp() {
        seatDAO = new SeatDAO();
    }

    // M24: 좌석 배치도 조회 테스트
    @Test
    public void testSelectSeatsByLayoutId() {

        long testSeatLayoutId = 1L;

        try {
            List<Seat> seats =
                    seatDAO.selectSeatsByLayoutId(testSeatLayoutId);

            assertNotNull("좌석 목록이 null이면 안 됩니다.", seats);

            System.out.println("=== M24 좌석 배치도 조회 ===");
            System.out.println("조회 좌석 수: " + seats.size());

            for (Seat seat : seats) {
                System.out.println(seat);

                assertEquals(testSeatLayoutId,
                             seat.getSeatLayoutId());
            }

        } catch (SQLException e) {
            fail("M24 조회 실패: " + e.getMessage());
        }
    }

    // M25: 섹터별 잔여 좌석 조회 테스트
    @Test
    public void testSelectSectorAvailability() {

        long testSeatLayoutId = 1L;

        try {
            List<SectorAvailabilityDTO> sectors =
                    seatDAO.selectSectorAvailability(testSeatLayoutId);

            assertNotNull("섹터 목록이 null이면 안 됩니다.", sectors);

            System.out.println("=== M25 섹터별 잔여 좌석 조회 ===");

            for (SectorAvailabilityDTO sector : sectors) {

                System.out.println(sector);

                assertTrue(
                    "잔여 좌석 수는 음수일 수 없습니다.",
                    sector.getAvailableSeats() >= 0
                );

                assertTrue(
                    "잔여 좌석 수는 전체 좌석 수보다 클 수 없습니다.",
                    sector.getAvailableSeats()
                        <= sector.getTotalSeats()
                );
            }

        } catch (SQLException e) {
            fail("M25 조회 실패: " + e.getMessage());
        }
    }
}

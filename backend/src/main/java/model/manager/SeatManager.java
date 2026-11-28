package model.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.BookingSeatDAO;
import model.dao.JDBCUtil;
import model.dao.SeatLayoutDAO;
import model.dao.VenueDAO;
import model.dto.PerformanceRegisterRequest.LayoutInput;
import model.dto.PerformanceRegisterRequest.SectorInput;
import model.dto.PerformanceRegisterRequest.SeatInput;

@Service
public class SeatManager {
    private final BookingSeatDAO bookingSeatDAO;
    private final SeatLayoutDAO seatLayoutDAO;

    public SeatManager(BookingSeatDAO bookingSeatDAO, SeatLayoutDAO seatLayoutDAO) {
        this.bookingSeatDAO = bookingSeatDAO;
        this.seatLayoutDAO = seatLayoutDAO;
    }

    /** 좌석 선점 + 예매 연결. 하나라도 실패하면 예외 → 호출자가 전체 롤백 */
    public void holdSeats(JDBCUtil j, Long performanceId, Long bookingId, List<Long> seatIds) throws Exception {
        List<Long> sorted = new ArrayList<>(seatIds);
        Collections.sort(sorted);

        for (Long seatId : sorted) {
            var price = bookingSeatDAO.selectSeatPrice(j, seatId, performanceId);
            if (price == null) {
                throw new ApiException(ErrorCode.NOT_FOUND, "해당 공연의 좌석이 아닙니다.");
            }
            if (bookingSeatDAO.updateSeatToHeld(j, seatId) == 0) {
                throw new ApiException(ErrorCode.SEAT_ALREADY_HELD);
            }
            bookingSeatDAO.insertBookingSeat(j, bookingId, seatId, price.doubleValue());
        }
    }

    /** 예매의 선점중(H) 좌석을 예매가능(A)으로 복원 */
    public int releaseSeats(JDBCUtil j, Long bookingId) throws Exception {
        return bookingSeatDAO.releaseSeatsByBookingId(j, bookingId);
    }

    /** M20 좌석 배치 저장 */
    public long createLayout(JDBCUtil jdbcUtil, long performanceId, Long venueTemplateId,
                             String venueName, String venueAddress,
                             LayoutInput layout, Map<String, Long> gradeIdByKey) throws Exception {
        long venueId;
        if (venueTemplateId != null) {
            venueId = venueTemplateId;
        } else {
            int total = 0;
            for (SectorInput s : layout.getSectors()) total += s.getSeats().size();
            venueId = VenueDAO.createVenue(jdbcUtil, venueName, venueAddress, total);
        }

        long layoutId = seatLayoutDAO.createSeatLayout(jdbcUtil, layout.getSeatLayoutName(),
                venueId, layout.getSourceLayoutId(), layout.isShareAsTemplate());

        for (SectorInput s : layout.getSectors()) {
            long sectorId = seatLayoutDAO.createSector(jdbcUtil, layoutId, s.getSectorName(),
                    gradeIdByKey.get(s.getGradeKey()), performanceId);
            for (SeatInput seat : s.getSeats()) {
                seatLayoutDAO.insertSeat(jdbcUtil, layoutId, sectorId,
                        seat.getRowNum(), seat.getColumnNum(), seat.getSeatNumber());
            }
        }
        return layoutId;
    }

    /** DB 저장 전 검증 */
    public void validateLayout(LayoutInput layout, Set<String> gradeKeys) {
        if (layout == null || layout.getSectors() == null || layout.getSectors().isEmpty())
            throw bad("구역이 하나 이상 필요합니다.");
        if (layout.getSeatLayoutName() == null || layout.getSeatLayoutName().isBlank()
                || layout.getSeatLayoutName().length() > 50)
            throw bad("좌석 배치 이름은 1~50자여야 합니다.");

        Set<String> usedPositions = new HashSet<>();
        for (SectorInput s : layout.getSectors()) {
            if (s.getSectorName() == null || s.getSectorName().isBlank() || s.getSectorName().length() > 50)
                throw bad("구역 이름은 1~50자여야 합니다.");
            if (!gradeKeys.contains(s.getGradeKey()))
                throw bad("구역 '" + s.getSectorName() + "'의 등급이 등급 목록에 없습니다.");
            if (s.getSeats() == null || s.getSeats().isEmpty())
                throw bad("구역 '" + s.getSectorName() + "'에 좌석이 없습니다.");
            for (SeatInput seat : s.getSeats()) {
                if (seat.getRowNum() == null || seat.getRowNum().isBlank() || seat.getRowNum().length() > 10)
                    throw bad("행 번호는 1~10자여야 합니다.");
                if (seat.getSeatNumber() == null || seat.getSeatNumber().isBlank() || seat.getSeatNumber().length() > 20)
                    throw bad("좌석 번호는 1~20자여야 합니다.");
                if (!usedPositions.add(seat.getRowNum() + "-" + seat.getColumnNum()))
                    throw bad("좌석 위치가 중복되었습니다: " + seat.getRowNum() + "행 " + seat.getColumnNum() + "열");
            }
        }
    }

    private ApiException bad(String message) {
        return new ApiException(ErrorCode.BAD_REQUEST, message);
    }
}
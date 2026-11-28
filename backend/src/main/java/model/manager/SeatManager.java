package model.manager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.JDBCUtil;
import model.dao.SeatLayoutDAO;
import model.dao.VenueDAO;
import model.dto.PerformanceRegisterRequest.LayoutInput;
import model.dto.PerformanceRegisterRequest.SectorInput;
import model.dto.PerformanceRegisterRequest.SeatInput;

@Service
public class SeatManager {
    private final SeatLayoutDAO seatLayoutDAO;

    public SeatManager(SeatLayoutDAO seatLayoutDAO) {
        this.seatLayoutDAO = seatLayoutDAO;
    }

    /** M20 좌석 배치 저장: (VENUE) → 레이아웃 → 구역 → 좌석. 트랜잭션은 호출한 PerformanceManager가 관리 */
    public long createLayout(JDBCUtil jdbcUtil, long performanceId, Long venueTemplateId,
                             String venueName, String venueAddress,
                             LayoutInput layout, Map<String, Long> gradeIdByKey) throws Exception {
        long venueId;
        if (venueTemplateId != null) {
            venueId = venueTemplateId;                       // 템플릿의 공연장에 붙인다
        } else {
            int total = 0;                                   // 직접 그렸으면 공연장 행을 새로 만든다
            for (SectorInput s : layout.getSectors()) total += s.getSeats().size();
            venueId = VenueDAO.createVenue(jdbcUtil, venueName, venueAddress, total);
        }

        long layoutId = seatLayoutDAO.createSeatLayout(jdbcUtil, layout.getSeatLayoutName(),
                venueId, layout.getSourceLayoutId(), layout.isShareAsTemplate());

        for (SectorInput s : layout.getSectors()) {
            long sectorId = seatLayoutDAO.createSector(jdbcUtil, layoutId, s.getSectorName(),
                    gradeIdByKey.get(s.getGradeKey()), performanceId);
            for (SeatInput seat : s.getSeats()) {
                // 좌석 상태는 항상 AVAILABLE (insertSeat 안에서 고정): 템플릿을 복사해도 상태는 따라오지 않는다
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

        Set<String> usedPositions = new HashSet<>();   // 레이아웃 전체에서 (행, 열) 중복 금지
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
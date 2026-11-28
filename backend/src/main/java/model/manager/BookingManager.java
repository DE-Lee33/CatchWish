package model.manager;

import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.BookingDAO;
import model.dao.JDBCUtil;
import model.dto.BookingHoldResponse;

@Component
public class BookingManager {
    private static final Logger log = LoggerFactory.getLogger(BookingManager.class);

    private final BookingDAO bookingDAO;
    private final SeatManager seatManager;

    public BookingManager(BookingDAO bookingDAO, SeatManager seatManager) {
        this.bookingDAO = bookingDAO;
        this.seatManager = seatManager;
    }

    /** 좌석 선점 */
    public BookingHoldResponse hold(Long memberId, Long performanceId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()
                || new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        JDBCUtil j = new JDBCUtil();
        try {
            // 1) 예매 입장 권한 (정책 미정 → 지금은 통과)
            checkEntry(j, memberId, performanceId);

            // 2) 이 회원의 같은 공연 기존 선점은 해제 (뒤로 가기 후 재선택 대응)
            for (Long oldId : bookingDAO.selectWaitingBookingIds(j, memberId, performanceId)) {
                if (bookingDAO.cancelWaitingBooking(j, oldId) == 1) {
                    seatManager.releaseSeats(j, oldId);
                }
            }

            // 3) 매수 제한: 이미 확정한 매수 + 이번 요청 매수
            int max = bookingDAO.selectMaxTicketsPerMember(j, performanceId);
            if (max < 0) throw new ApiException(ErrorCode.NOT_FOUND, "공연을 찾을 수 없습니다.");
            int already = bookingDAO.countConfirmedSeats(j, memberId, performanceId);
            if (already + seatIds.size() > max) {
                throw new ApiException(ErrorCode.MAX_TICKETS_EXCEEDED,
                        "최대 " + max + "매까지 예매 가능합니다.");
            }

            // 4) 예매 생성(W, HOLD_EXPIRES_AT 포함) + 좌석 선점. 중복 선점이면 예외 → 롤백
            Long bookingId = bookingDAO.insertHoldBooking(j, memberId, performanceId);
            seatManager.holdSeats(j, performanceId, bookingId, seatIds);

            int remaining = bookingDAO.selectRemainingSeconds(j, bookingId);
            j.commit();

            return new BookingHoldResponse(bookingId, performanceId, seatIds.stream().sorted().toList(),
                    System.currentTimeMillis() + remaining * 1000L, remaining);

        } catch (ApiException e) {
            rollbackQuietly(j);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(j);
            log.error("좌석 선점 중 오류", e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        } finally {
            j.close();
        }
    }

    /** 홀드 만료 처리 (스케줄러가 호출). 만료 처리한 예매 수 반환 */
    public int expireHolds() {
        int count = 0;
        JDBCUtil j = new JDBCUtil();
        try {
            List<Long> ids = bookingDAO.selectExpiredWaitingIds(j);
            for (Long id : ids) {
                try {
                    // 조건부 UPDATE: 그 사이 결제 확정됐다면 0행 → 좌석도 건드리지 않음
                    if (bookingDAO.cancelWaitingBooking(j, id) == 1) {
                        seatManager.releaseSeats(j, id);
                        count++;
                    }
                    j.commit();                       // 예매 단위 commit → 한 건 실패가 다른 건에 영향 없음
                } catch (Exception e) {
                    rollbackQuietly(j);
                    log.error("홀드 만료 처리 실패 bookingId={}", id, e);
                }
            }
        } catch (Exception e) {
            log.error("홀드 만료 목록 조회 실패", e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        } finally {
            j.close();
        }
        return count;
    }

    /**
     * 예매 입장 권한 확인.
     * TODO: 예매 오픈 시간(BOOKING_NOT_OPEN), 대기열 QUEUE.STATUS='A'(ENTRY_REQUIRED),
     *       선예매 코드, 공연 승인 상태(APPROVAL_STATUS='A') 등 정책 확정 시 여기에 구현.
     */
    private void checkEntry(JDBCUtil j, Long memberId, Long performanceId) throws Exception {
        // 지금은 통과
    }

    private void rollbackQuietly(JDBCUtil j) {
        try { j.rollback(); } catch (Exception ignore) { }
    }
}
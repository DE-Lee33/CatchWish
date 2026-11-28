package model.manager;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.CanceledTicketScheduleDAO;
import model.dao.PerformanceDAO;
import model.domain.CanceledTicketSchedule;
import model.domain.Performance;

@Service
public class CanceledTicketScheduleManager {
    private final PerformanceDAO performanceDAO;
    private final CanceledTicketScheduleDAO scheduleDAO;

    public CanceledTicketScheduleManager(
            PerformanceDAO performanceDAO,
            CanceledTicketScheduleDAO scheduleDAO) {
        this.performanceDAO = performanceDAO;
        this.scheduleDAO = scheduleDAO;
    }
    
    // 취소표 재오픈 일정 등록 (M27)
    public void registerSchedule(long performanceId, Timestamp openAt, long loginMemberId)
            throws Exception {
        if (openAt == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "재오픈 일시를 입력해 주세요.");
        }
        if (!openAt.toInstant().isAfter(Instant.now())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "재오픈 일시는 현재보다 이후여야 합니다.");
        }

        Performance performance = performanceDAO.findById(performanceId);
        if (performance == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "공연을 찾을 수 없습니다.");
        }
        
        // 로그인한 회원이 해당 공연을 등록했는지 확인
        if (performance.getRegistrantId() != loginMemberId) {
            throw new ApiException(ErrorCode.FORBIDDEN, "공연 등록자만 재오픈 일정을 설정할 수 있습니다.");
        }

        CanceledTicketSchedule schedule = new CanceledTicketSchedule();
        schedule.setPerformanceId(performanceId);
        schedule.setOpenAt(openAt);

        // DAO에서 일정을 대기 상태(P)로 저장하고 커밋
        if (scheduleDAO.insertSchedule(schedule) != 1) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "재오픈 일정을 저장하지 못했습니다.");
        }
    }
}
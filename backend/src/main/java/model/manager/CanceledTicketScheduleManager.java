package model.manager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.CanceledTicketScheduleDAO;
import model.dao.JDBCUtil;
import model.dao.PerformanceDAO;
import model.domain.CanceledTicketSchedule;
import model.domain.Performance;


@Service
public class CanceledTicketScheduleManager {
    private final PerformanceDAO performanceDAO;
    private final CanceledTicketScheduleDAO scheduleDAO;
    private final NotificationManager notificationManager;

    public CanceledTicketScheduleManager(
            PerformanceDAO performanceDAO,
            CanceledTicketScheduleDAO scheduleDAO,
            NotificationManager notificationManager) {
        this.performanceDAO = performanceDAO;
        this.scheduleDAO = scheduleDAO;
        this.notificationManager = notificationManager;
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

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            long scheduleId = scheduleDAO.insertScheduleAndGetId(jdbcUtil, schedule);
            notificationManager.createCancelTicketNotifications(jdbcUtil, performanceId, scheduleId);

            // 일정과 알림 저장이 모두 성공하면 함께 커밋한다.
            jdbcUtil.commit();
        } catch (Exception e) {
            try {
                jdbcUtil.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        } finally {
            jdbcUtil.close();
        }
    }
}
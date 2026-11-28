package model.manager;

import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Instant;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.CanceledTicketScheduleDAO;
import model.dao.PerformanceDAO;
import model.dao.JDBCUtil;
import model.dao.NotificationDAO;
import model.domain.Notification;
import model.domain.Wishlist;
import model.domain.CanceledTicketSchedule;
import model.domain.Performance;


@Service
public class CanceledTicketScheduleManager {
    private final PerformanceDAO performanceDAO;
    private final CanceledTicketScheduleDAO scheduleDAO;
    private final NotificationDAO notificationDAO;

    public CanceledTicketScheduleManager(
            PerformanceDAO performanceDAO,
            CanceledTicketScheduleDAO scheduleDAO,
            NotificationDAO notificationDAO) {
        this.performanceDAO = performanceDAO;
        this.scheduleDAO = scheduleDAO;
        this.notificationDAO = notificationDAO;
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

        List<Long> sectorIds = notificationDAO.selectCancelPendingSectorIds(performanceId);
        Set<Long> notifiedWishlistIds = new HashSet<>();
        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            long scheduleId = scheduleDAO.insertScheduleAndGetId(jdbcUtil, schedule);

            for (Long sectorId : sectorIds) {
                List<Wishlist> targets =
                        notificationDAO.selectWishlistTargets(jdbcUtil, scheduleId, sectorId);

                for (Wishlist wishlist : targets) {
                    // 구역을 지정하지 않은 위시리스트가 여러 구역에서 조회될 수 있다.
                    if (!notifiedWishlistIds.add(wishlist.getWishlistId())) {
                        continue;
                    }

                    Notification notification = new Notification();
                    notification.setNotificationType("CANCEL");
                    notification.setMemberId(wishlist.getMemberId());
                    notification.setWishlistId(wishlist.getWishlistId());
                    notification.setCanceledTicketScheduleId(scheduleId);

                    if (notificationDAO.insertNotification(jdbcUtil, notification) != 1) {
                        throw new SQLException("취소표 알림을 저장하지 못했습니다.");
                    }
                }
            }

            // 일정과 알림 저장이 모두 성공하면 한 번에 확정한다.
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
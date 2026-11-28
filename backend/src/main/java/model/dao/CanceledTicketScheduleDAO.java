package model.dao;

import java.sql.SQLException; 

import org.springframework.stereotype.Repository;

import model.domain.CanceledTicketSchedule;
import model.domain.enums.ScheduleStatus;
import java.sql.ResultSet;

@Repository
public class CanceledTicketScheduleDAO {
    // 취소표 재오픈 일정 등록 (M27)
    public int insertSchedule(CanceledTicketSchedule schedule) {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            int result = executeInsert(jdbcUtil, schedule);
            jdbcUtil.commit();
            return result;
        } catch (Exception e) {
            try { jdbcUtil.rollback(); } catch (SQLException ignore) { }
            throw new RuntimeException("취소표 재오픈 일정 등록 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }
    
    private int executeInsert(JDBCUtil jdbcUtil, CanceledTicketSchedule schedule)
            throws Exception {
        String sql = "INSERT INTO CANCELED_TICKET_SCHEDULE "
                + "(CANCELED_TICKET_SCHEDULE_ID, OPEN_AT, "
                + "SCHEDULE_STATUS, CREATED_AT, PERFORMANCE_ID) "
                + "VALUES (SEQ_CANCELED_TICKET_SCHEDULE.NEXTVAL, "
                + "?, ?, SYSDATE, ?)";

        Object[] params = {
                schedule.getOpenAt(),
                ScheduleStatus.PLANNED.getCode(),
                schedule.getPerformanceId()
        };

        jdbcUtil.setSqlAndParameters(sql, params);
        return jdbcUtil.executeUpdate();
    }
    
    // 알림에 연결할 수 있도록 방금 저장한 일정의 ID를 반환한다.(M28)
    public long insertScheduleAndGetId(
            JDBCUtil jdbcUtil, CanceledTicketSchedule schedule) throws Exception {
        if (executeInsert(jdbcUtil, schedule) != 1) {
            throw new SQLException("재오픈 일정을 저장하지 못했습니다.");
        }

        jdbcUtil.setSqlAndParameters(
                "SELECT SEQ_CANCELED_TICKET_SCHEDULE.CURRVAL FROM DUAL",
                new Object[0]);

        try (ResultSet rs = jdbcUtil.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("저장한 재오픈 일정 ID를 가져오지 못했습니다.");
            }
            return rs.getLong(1);
        }
    }
}
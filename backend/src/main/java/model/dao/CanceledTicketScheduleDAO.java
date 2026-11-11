package model.dao;

import model.domain.CanceledTicketSchedule;

public class CanceledTicketScheduleDAO {
    // 취소표 재오픈 일정 등록 (M27)
    public int insertSchedule(CanceledTicketSchedule schedule) {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "INSERT INTO CANCELED_TICKET_SCHEDULE "
                + "(CANCELED_TICKET_SCHEDULE_ID, OPEN_AT, "
                + "SCHEDULE_STATUS, CREATED_AT, PERFORMANCE_ID) "
                + "VALUES (SEQ_CANCELED_TICKET_SCHEDULE.NEXTVAL, "
                + "?, 'P', SYSDATE, ?)";

        Object[] params = {
                schedule.getOpenAt(),
                schedule.getPerformanceId()
        };

        try {
            jdbcUtil.setSqlAndParameters(sql, params);

            int result = jdbcUtil.executeUpdate();

            jdbcUtil.commit();

            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("취소표 재오픈 일정 등록 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }
}

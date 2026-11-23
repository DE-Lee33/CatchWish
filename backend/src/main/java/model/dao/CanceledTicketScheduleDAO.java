/*
 * [변경 요약] CanceledTicketScheduleDAO  (표시: [수정] [추가])
 * [추가] import java.sql.SQLException
 * [수정] catch 블록의 jdbcUtil.rollback()을 try/catch로 감쌈 (컴파일 에러 해결, 원래 예외 보존)
 */
package model.dao;

import java.sql.SQLException;   // [추가]

import org.springframework.stereotype.Repository;

import model.domain.CanceledTicketSchedule;

@Repository
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
            try { jdbcUtil.rollback(); } catch (SQLException ignore) { }   // [수정]
            throw new RuntimeException("취소표 재오픈 일정 등록 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }
}
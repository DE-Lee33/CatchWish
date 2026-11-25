package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.stereotype.Repository;

import model.domain.Queue;
import model.domain.enums.QueueStatus;
import model.dto.QueueStatusDTO;

@Repository
public class QueueDAO {

    // 1. 대기열 진입
    public Long insertQueue(Long memberId, Long performanceId)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        try {
            // 1) 대기 순번으로 사용할 시퀀스 번호 발급
            String sequenceSql =
                    "SELECT SEQ_QUEUE.NEXTVAL AS QUEUE_ID FROM DUAL";

            jdbcUtil.setSqlAndParameters(
                    sequenceSql,
                    new Object[] {}
            );

            ResultSet rs = jdbcUtil.executeQuery();

            if (rs == null || !rs.next()) {
                jdbcUtil.rollback();
                return null;
            }

            Long queueId = rs.getLong("QUEUE_ID");

            // 2) 발급받은 번호를 QUEUE_ID와 POSITION에 함께 사용
            String sql =
                    "INSERT INTO QUEUE " +
                    "(QUEUE_ID, POSITION, STATUS, CREATED_AT, MEMBER_ID, PERFORMANCE_ID) " +
                    "VALUES (?, ?, ?, SYSDATE, ?, ?)";

            jdbcUtil.setSqlAndParameters(
                    sql,
                    new Object[] {
                            queueId,
                            queueId,
                            QueueStatus.WAITING.getCode(),
                            memberId,
                            performanceId
                    }
            );

            int result = jdbcUtil.executeUpdate();

            if (result == 0) {
                jdbcUtil.rollback();
                return null;
            }

            jdbcUtil.commit();

            return queueId;

        } catch (Exception ex) {
            try { jdbcUtil.rollback(); } catch (SQLException ignore) { } 
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    // 2. 기존 대기열 확인
    public Queue selectActiveQueue(Long memberId, Long performanceId)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql =
                "SELECT QUEUE_ID, POSITION, STATUS " +
                "FROM QUEUE " +
                "WHERE MEMBER_ID = ? AND PERFORMANCE_ID = ? " +
                "AND STATUS IN (?, ?)";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    memberId, performanceId,
                    QueueStatus.WAITING.getCode(), QueueStatus.TURN.getCode()
            });

            ResultSet rs = jdbcUtil.executeQuery();

            if (rs != null && rs.next()) {
                return new Queue(
                        rs.getLong("QUEUE_ID"),
                        rs.getInt("POSITION"),
                        QueueStatus.fromCode(rs.getString("STATUS"))
                );
            }

            return null;

        } finally {
            jdbcUtil.close();
        }
    }

    // 3. 대기 순번 및 전체 대기자 수 조회
    public QueueStatusDTO selectQueueStatus(Long queueId)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql =
                "SELECT q.QUEUE_ID, q.POSITION, q.STATUS, " +
                "CASE " +
                "WHEN q.STATUS = ? THEN 0 " +
                "WHEN q.STATUS = ? THEN (" +
                "SELECT COUNT(*) + 1 " +
                "FROM QUEUE q2 " +
                "WHERE q2.PERFORMANCE_ID = q.PERFORMANCE_ID " +
                "AND q2.STATUS = ? " +
                "AND q2.POSITION < q.POSITION) " +
                "ELSE 0 END AS WAITING_POSITION, " +
                "(SELECT COUNT(*) " +
                "FROM QUEUE q3 " +
                "WHERE q3.PERFORMANCE_ID = q.PERFORMANCE_ID " +
                "AND q3.STATUS = ?) AS TOTAL_WAITING " +
                "FROM QUEUE q " +
                "WHERE q.QUEUE_ID = ?";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    QueueStatus.TURN.getCode(), QueueStatus.WAITING.getCode(), QueueStatus.WAITING.getCode(), QueueStatus.WAITING.getCode(),
                    queueId
            });

            ResultSet rs = jdbcUtil.executeQuery();

            if (rs != null && rs.next()) {
                return new QueueStatusDTO(
                        rs.getLong("QUEUE_ID"),
                        rs.getInt("POSITION"),
                        QueueStatus.fromCode(rs.getString("STATUS")),
                        rs.getInt("WAITING_POSITION"),
                        rs.getInt("TOTAL_WAITING")
                );
            }

            return null;

        } finally {
            jdbcUtil.close();
        }
    }

    // 4. 대기 상태 변경
    public int updateQueueStatus(Long queueId, QueueStatus status)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql =
                "UPDATE QUEUE SET STATUS = ? WHERE QUEUE_ID = ?";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    status.getCode(), queueId
            });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            try { jdbcUtil.rollback(); } catch (Exception ignore) { }  
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    // 5. 인증번호 발급
    public int issueAuthCode(Long queueId, String authCode)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql =
                "UPDATE QUEUE " +
                "SET AUTH_CODE = ?, IS_VERIFIED = 'N' " +
                "WHERE QUEUE_ID = ? AND STATUS = ?";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    authCode, queueId, QueueStatus.TURN.getCode()
            });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            try { jdbcUtil.rollback(); } catch (Exception ignore) { }  
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    // 6. 인증번호 확인
    public int verifyAuthCode(Long queueId, String authCode)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql =
                "UPDATE QUEUE " +
                "SET IS_VERIFIED = 'Y' " +
                "WHERE QUEUE_ID = ? " +
                "AND AUTH_CODE = ? " +
                "AND STATUS = ? " +
                "AND IS_VERIFIED = 'N'";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    queueId, authCode, QueueStatus.TURN.getCode()
            });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            try { jdbcUtil.rollback(); } catch (Exception ignore) { }  
            throw ex;
        } finally {
            jdbcUtil.close();
        }
    }

    // 7. 대기열 인증 여부 조회
    public boolean isVerified(Long queueId) throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql =
                "SELECT IS_VERIFIED " +
                "FROM QUEUE " +
                "WHERE QUEUE_ID = ? AND STATUS = ?";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    queueId, QueueStatus.TURN.getCode()
            });

            ResultSet rs = jdbcUtil.executeQuery();

            if (rs != null && rs.next()) {
                return "Y".equals(rs.getString("IS_VERIFIED"));
            }

            return false;

        } finally {
            jdbcUtil.close();
        }
    }
}
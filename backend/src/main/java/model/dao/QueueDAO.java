
package model.dao;

import java.sql.ResultSet;

import model.domain.Queue;
import model.dto.QueueStatusDTO;

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
                    "VALUES (?, ?, 'W', SYSDATE, ?, ?)";

            jdbcUtil.setSqlAndParameters(
                    sql,
                    new Object[] {
                            queueId,
                            queueId,
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
            jdbcUtil.rollback();
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
                "AND STATUS IN ('W', 'A')";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    memberId, performanceId
            });

            ResultSet rs = jdbcUtil.executeQuery();

            if (rs != null && rs.next()) {
                return new Queue(
                        rs.getLong("QUEUE_ID"),
                        rs.getInt("POSITION"),
                        rs.getString("STATUS")
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
                "WHEN q.STATUS = 'A' THEN 0 " +
                "WHEN q.STATUS = 'W' THEN (" +
                "SELECT COUNT(*) + 1 " +
                "FROM QUEUE q2 " +
                "WHERE q2.PERFORMANCE_ID = q.PERFORMANCE_ID " +
                "AND q2.STATUS = 'W' " +
                "AND q2.POSITION < q.POSITION) " +
                "ELSE 0 END AS WAITING_POSITION, " +
                "(SELECT COUNT(*) " +
                "FROM QUEUE q3 " +
                "WHERE q3.PERFORMANCE_ID = q.PERFORMANCE_ID " +
                "AND q3.STATUS = 'W') AS TOTAL_WAITING " +
                "FROM QUEUE q " +
                "WHERE q.QUEUE_ID = ?";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    queueId
            });

            ResultSet rs = jdbcUtil.executeQuery();

            if (rs != null && rs.next()) {
                return new QueueStatusDTO(
                        rs.getLong("QUEUE_ID"),
                        rs.getInt("POSITION"),
                        rs.getString("STATUS"),
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
    public int updateQueueStatus(Long queueId, String status)
            throws Exception {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql =
                "UPDATE QUEUE SET STATUS = ? WHERE QUEUE_ID = ?";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    status, queueId
            });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            jdbcUtil.rollback();
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
                "WHERE QUEUE_ID = ? AND STATUS = 'A'";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    authCode, queueId
            });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            jdbcUtil.rollback();
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
                "AND STATUS = 'A' " +
                "AND IS_VERIFIED = 'N'";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    queueId, authCode
            });

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();

            return result;

        } catch (Exception ex) {
            jdbcUtil.rollback();
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
                "WHERE QUEUE_ID = ? AND STATUS = 'A'";

        try {
            jdbcUtil.setSqlAndParameters(sql, new Object[] {
                    queueId
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

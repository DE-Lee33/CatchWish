package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.domain.CheckAuthCode;
import model.domain.PresaleCode;

public class PresaleCodeDAO {
    // 선예매 코드 등록
    public int insertPresaleCode(PresaleCode code) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "INSERT INTO PRESALE_CODE "
                + "(CODE_ID, PERFORMANCE_ID, CODE_VALUE, SEAT_LIMIT, "
                + "START_AT, END_AT, CREATED_AT) "
                + "VALUES (SEQ_PRESALE_CODE.NEXTVAL, ?, ?, ?, ?, ?, SYSDATE)";

        Object[] params = {
                code.getPerformanceId(),
                code.getCodeValue(),
                code.getSeatLimit(),
                code.getStartAt(),
                code.getEndAt()
        };

        try {
            jdbcUtil.setSqlAndParameters(sql, params);
            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();
            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("선예매 코드 등록 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 선예매 코드 중복 확인
    public boolean existsCode(Long performanceId, String codeValue) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "SELECT COUNT(*) FROM PRESALE_CODE "
                + "WHERE PERFORMANCE_ID = ? AND CODE_VALUE = ?";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{performanceId, codeValue});

            ResultSet rs = jdbcUtil.executeQuery();
            if (rs == null) {
                throw new SQLException("코드 중복 조회 실패");
            }

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

            return false;

        } catch (Exception e) {
            throw new RuntimeException("선예매 코드 중복 확인 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 유효한 선예매 코드 조회
    public PresaleCode selectValidCode(
            Long performanceId, String codeValue) {

        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "SELECT * FROM PRESALE_CODE "
                + "WHERE PERFORMANCE_ID = ? "
                + "AND CODE_VALUE = ? "
                + "AND START_AT <= SYSDATE "
                + "AND END_AT > SYSDATE";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{performanceId, codeValue});

            ResultSet rs = jdbcUtil.executeQuery();
            if (rs == null) {
                throw new SQLException("유효 코드 조회 실패");
            }

            if (rs.next()) {
                return mapPresaleCode(rs);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("선예매 코드 유효성 확인 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 공연별 선예매 코드 목록 조회
    public List<PresaleCode> selectPresaleCodeByPerformanceId(
            Long performanceId) {

        JDBCUtil jdbcUtil = new JDBCUtil();
        List<PresaleCode> codes = new ArrayList<>();

        String sql = "SELECT * FROM PRESALE_CODE "
                + "WHERE PERFORMANCE_ID = ? "
                + "ORDER BY CREATED_AT DESC";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{performanceId});

            ResultSet rs = jdbcUtil.executeQuery();
            if (rs == null) {
                throw new SQLException("공연별 코드 조회 실패");
            }

            while (rs.next()) {
                codes.add(mapPresaleCode(rs));
            }

            return codes;

        } catch (Exception e) {
            throw new RuntimeException("공연별 선예매 코드 조회 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 선예매 코드 정보 수정
    public int updatePresaleCode(PresaleCode code) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "UPDATE PRESALE_CODE "
                + "SET CODE_VALUE = ?, SEAT_LIMIT = ?, "
                + "START_AT = ?, END_AT = ? "
                + "WHERE CODE_ID = ? AND PERFORMANCE_ID = ?";

        Object[] params = {
                code.getCodeValue(),
                code.getSeatLimit(),
                code.getStartAt(),
                code.getEndAt(),
                code.getCodeId(),
                code.getPerformanceId()
        };

        try {
            jdbcUtil.setSqlAndParameters(sql, params);
            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();
            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("선예매 코드 수정 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 선예매 코드 삭제
    public int deletePresaleCode(Long codeId, Long performanceId) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "DELETE FROM PRESALE_CODE "
                + "WHERE CODE_ID = ? AND PERFORMANCE_ID = ?";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{codeId, performanceId});

            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();
            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("선예매 코드 삭제 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 회원 선예매 인증 기록 생성
    public int insertVerification(CheckAuthCode authCode) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "INSERT INTO CHECK_AUTH_CODE "
                + "(AUTH_CODE_ID, MEMBER_ID, PERFORMANCE_ID, "
                + "IS_VERIFIED, EXPIRES_AT, CREATED_AT) "
                + "VALUES (SEQ_AUTH_CODE.NEXTVAL, ?, ?, 'Y', ?, SYSDATE)";

        Object[] params = {
                authCode.getMemberId(),
                authCode.getPerformanceId(),
                authCode.getExpiresAt()
        };

        try {
            jdbcUtil.setSqlAndParameters(sql, params);
            int result = jdbcUtil.executeUpdate();
            jdbcUtil.commit();
            return result;

        } catch (Exception e) {
            jdbcUtil.rollback();
            throw new RuntimeException("회원 선예매 인증 기록 생성 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // 회원 선예매 인증 여부 확인
    public boolean isVerified(Long memberId, Long performanceId) {
        JDBCUtil jdbcUtil = new JDBCUtil();

        String sql = "SELECT COUNT(*) FROM CHECK_AUTH_CODE "
                + "WHERE MEMBER_ID = ? "
                + "AND PERFORMANCE_ID = ? "
                + "AND IS_VERIFIED = 'Y' "
                + "AND EXPIRES_AT > SYSDATE";

        try {
            jdbcUtil.setSqlAndParameters(
                    sql, new Object[]{memberId, performanceId});

            ResultSet rs = jdbcUtil.executeQuery();
            if (rs == null) {
                throw new SQLException("인증 여부 조회 실패");
            }

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

            return false;

        } catch (Exception e) {
            throw new RuntimeException("회원 선예매 인증 확인 실패", e);

        } finally {
            jdbcUtil.close();
        }
    }

    // ResultSet을 PresaleCode 객체로 변환
    private PresaleCode mapPresaleCode(ResultSet rs)
            throws SQLException {

        PresaleCode code = new PresaleCode();

        code.setCodeId(rs.getLong("CODE_ID"));
        code.setPerformanceId(rs.getLong("PERFORMANCE_ID"));
        code.setCodeValue(rs.getString("CODE_VALUE"));
        code.setSeatLimit(rs.getInt("SEAT_LIMIT"));
        code.setStartAt(rs.getTimestamp("START_AT"));
        code.setEndAt(rs.getTimestamp("END_AT"));
        code.setCreatedAt(rs.getTimestamp("CREATED_AT"));

        return code;
    }
}

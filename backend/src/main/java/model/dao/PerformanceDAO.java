package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.domain.Performance;
import model.domain.SeatGrade;
import model.domain.VenueRentalProof;
import model.dto.MyPerformanceSummary;

/**
 * 공연 DAO (PERFORMANCE, SEAT_GRADE, VENUE_RENTAL_PROOF 테이블)
 * 등급·대관 증빙은 공연에 종속되어 공연 등록·수정·심사 때 함께 다루므로 한 DAO에서 관리한다.
 *
 * 담당 기능: M4 공연 목록, M5 공연 검색, M6 공연 상세, M8 내가 등록한 공연,
 *           M9 공연 등록, M10 공연 수정, M11 공연 삭제, M30 관리자 심사
 *
 * 참고: M9처럼 공연·등급·증빙·좌석을 한 번에 넣는 작업은 Manager 단계에서 하나의 트랜잭션으로 묶어야 한다.
 *      지금은 팀 규칙대로 메서드마다 commit한다.
 */
public class PerformanceDAO {
	private static final String STATUS_APPROVED = Performance.STATUS_APPROVED;
	private static final String STATUS_PENDING = Performance.STATUS_PENDING;
	private static final String STATUS_REJECTED = Performance.STATUS_REJECTED;
	/** 좌석 상태 코드 A: 예매 가능 */
	private static final String SEAT_AVAILABLE = "A";

	private JDBCUtil jdbcUtil = null;

	public PerformanceDAO() {
		jdbcUtil = new JDBCUtil();
	}

	// ===================== M4·M5 목록·검색 (승인된 공연만) =====================

	/**
	 * M4 승인된 공연 목록 (공연 일시 순, 페이징)
	 * @param genre 장르 (null 또는 빈 문자열이면 전체)
	 * @param page  1부터 시작
	 * @param size  한 페이지 개수
	 */
	public List<Performance> findApprovedList(String genre, int page, int size) throws SQLException {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("WHERE APPROVAL_STATUS = ? ");
		params.add(STATUS_APPROVED);
		appendGenre(where, params, genre);
		return findPage(where.toString(), params, page, size);
	}

	/** M4 승인된 공연 수 (페이지 수 계산용) */
	public int countApproved(String genre) throws SQLException {
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM PERFORMANCE WHERE APPROVAL_STATUS = ? ");
		params.add(STATUS_APPROVED);
		appendGenre(sql, params, genre);
		return queryCount(sql.toString(), params.toArray());
	}

	/**
	 * M5 공연명·출연진 검색 (승인된 공연만, 페이징)
	 * @param keyword 검색어 (앞뒤에 %를 붙여서 부분 일치)
	 * @param genre   장르 (null 또는 빈 문자열이면 전체)
	 */
	public List<Performance> search(String keyword, String genre, int page, int size) throws SQLException {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder();
		appendSearchCondition(where, params, keyword, genre);
		return findPage(where.toString(), params, page, size);
	}

	/** M5 검색 결과 수 */
	public int countSearch(String keyword, String genre) throws SQLException {
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM PERFORMANCE ");
		appendSearchCondition(sql, params, keyword, genre);
		return queryCount(sql.toString(), params.toArray());
	}

	// ===================== M6 상세 =====================

	/** M6·M10 공연 상세: 없으면 null (승인 여부 판단은 Manager에서) */
	public Performance findById(long performanceId) throws SQLException {
		jdbcUtil.setSqlAndParameters("SELECT * FROM PERFORMANCE WHERE PERFORMANCE_ID = ?",
				new Object[] { performanceId });
		try {
			ResultSet rs = executeQueryOrThrow();
			return rs.next() ? mapPerformance(rs) : null;
		} finally {
			jdbcUtil.close();
		}
	}

	/** M6 좌석 등급·가격 목록 (비싼 등급부터) */
	public List<SeatGrade> findGradeList(long performanceId) throws SQLException {
		jdbcUtil.setSqlAndParameters(
				"SELECT GRADE_ID, PERFORMANCE_ID, GRADE_NAME, PRICE FROM SEAT_GRADE "
						+ "WHERE PERFORMANCE_ID = ? ORDER BY PRICE DESC",
				new Object[] { performanceId });
		try {
			ResultSet rs = executeQueryOrThrow();
			List<SeatGrade> list = new ArrayList<>();
			while (rs.next()) {
				SeatGrade grade = new SeatGrade();
				grade.setGradeId(rs.getLong("GRADE_ID"));
				grade.setPerformanceId(rs.getLong("PERFORMANCE_ID"));
				grade.setGradeName(rs.getString("GRADE_NAME"));
				grade.setPrice(rs.getInt("PRICE"));
				list.add(grade);
			}
			return list;
		} finally {
			jdbcUtil.close();
		}
	}

	// ===================== M8 내가 등록한 공연 =====================

	/** M8 내가 등록한 공연 + 전체 좌석 수·잔여석 수 (심사 상태와 무관하게 전부, 최근 등록 순) */
	public List<MyPerformanceSummary> findMyList(long registrantId) throws SQLException {
		String sql = "SELECT p.*, "
				+ "  (SELECT COUNT(*) FROM SEAT s JOIN SECTOR sc ON s.SECTOR_ID = sc.SECTOR_ID "
				+ "    WHERE sc.PERFORMANCE_ID = p.PERFORMANCE_ID) AS TOTAL_SEATS, "
				+ "  (SELECT COUNT(*) FROM SEAT s JOIN SECTOR sc ON s.SECTOR_ID = sc.SECTOR_ID "
				+ "    WHERE sc.PERFORMANCE_ID = p.PERFORMANCE_ID AND s.SEAT_STATUS = ?) AS AVAILABLE_SEATS "
				+ "FROM PERFORMANCE p "
				+ "WHERE p.REGISTRANT_ID = ? "
				+ "ORDER BY p.CREATED_AT DESC";
		jdbcUtil.setSqlAndParameters(sql, new Object[] { SEAT_AVAILABLE, registrantId });
		try {
			ResultSet rs = executeQueryOrThrow();
			List<MyPerformanceSummary> list = new ArrayList<>();
			while (rs.next()) {
				list.add(new MyPerformanceSummary(mapPerformance(rs),
						rs.getInt("TOTAL_SEATS"), rs.getInt("AVAILABLE_SEATS")));
			}
			return list;
		} finally {
			jdbcUtil.close();
		}
	}

	// ===================== M9 등록 =====================

	/** M9 공연 등록: 심사중(P) 상태로 저장하고 생성된 공연 번호를 반환 */
	public long create(Performance p) throws SQLException {
		String sql = "INSERT INTO PERFORMANCE (PERFORMANCE_ID, REGISTRANT_ID, VENUE_TEMPLATE_ID, PERFORMANCE_NAME, GENRE, "
				+ "CAST_INFO, DESCRIPTION, POSTER_URL, RUNNING_TIME, SHOW_AT, BOOKING_OPEN_AT, "
				+ "CANCEL_DEADLINE_AT, MAX_TICKETS_PER_MEMBER, VENUE_NAME, VENUE_ADDRESS, "
				+ "APPROVAL_STATUS, CREATED_AT) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";
		try {
			long performanceId = nextSequence("SEQ_PERFORMANCE");
			jdbcUtil.setSqlAndParameters(sql, new Object[] {
					performanceId, p.getRegistrantId(), p.getVenueTemplateId(),
					p.getPerformanceName(), p.getGenre(), p.getCastInfo(), p.getDescription(),
					p.getPosterUrl(), p.getRunningTime(), toTimestamp(p.getShowAt()),
					toTimestamp(p.getBookingOpenAt()), toTimestamp(p.getCancelDeadlineAt()),
					p.getMaxTicketsPerMember(), p.getVenueName(), p.getVenueAddress(),
					STATUS_PENDING });
			jdbcUtil.executeUpdate();
			jdbcUtil.commit();

			p.setPerformanceId(performanceId);
			p.setApprovalStatus(STATUS_PENDING);
			return performanceId;
		} catch (Exception ex) {
			rollbackQuietly();
			throw toSQLException("공연 등록 실패", ex);
		} finally {
			jdbcUtil.close();
		}
	}

	/** M9 좌석 등급 등록: 섹터가 이 등급을 참조하므로 생성된 등급 번호를 반환 */
	public long createGrade(SeatGrade grade) throws SQLException {
		String sql = "INSERT INTO SEAT_GRADE (GRADE_ID, PERFORMANCE_ID, GRADE_NAME, PRICE) VALUES (?, ?, ?, ?)";
		try {
			long gradeId = nextSequence("SEQ_SEAT_GRADE");
			jdbcUtil.setSqlAndParameters(sql, new Object[] {
					gradeId, grade.getPerformanceId(), grade.getGradeName(), grade.getPrice() });
			jdbcUtil.executeUpdate();
			jdbcUtil.commit();

			grade.setGradeId(gradeId);
			return gradeId;
		} catch (Exception ex) {
			rollbackQuietly();
			throw toSQLException("좌석 등급 등록 실패", ex);
		} finally {
			jdbcUtil.close();
		}
	}

	/** M9 대관 증빙 등록: 생성된 증빙 번호를 반환 */
	public long createRentalProof(VenueRentalProof proof) throws SQLException {
		String sql = "INSERT INTO VENUE_RENTAL_PROOF (PROOF_ID, PERFORMANCE_ID, FILE_PATH, ORIGINAL_FILE_NAME, "
				+ "RENTAL_START_AT, RENTAL_END_AT, SUBMITTED_AT) VALUES (?, ?, ?, ?, ?, ?, SYSDATE)";
		try {
			long proofId = nextSequence("SEQ_RENTAL_PROOF");
			jdbcUtil.setSqlAndParameters(sql, new Object[] {
					proofId, proof.getPerformanceId(), proof.getFilePath(), proof.getOriginalFileName(),
					toTimestamp(proof.getRentalStartAt()), toTimestamp(proof.getRentalEndAt()) });
			jdbcUtil.executeUpdate();
			jdbcUtil.commit();

			proof.setProofId(proofId);
			return proofId;
		} catch (Exception ex) {
			rollbackQuietly();
			throw toSQLException("대관 증빙 등록 실패", ex);
		} finally {
			jdbcUtil.close();
		}
	}

	// ===================== M10 수정 =====================

	/**
	 * M10 공연 정보 수정: 변경된 행 수를 반환
	 * 심사 상태(approvalStatus)는 Manager가 정해서 넘긴다 (예: 반려 공연을 수정하면 다시 P)
	 */
	public int update(Performance p) throws SQLException {
		String sql = "UPDATE PERFORMANCE "
				+ "SET VENUE_TEMPLATE_ID = ?, PERFORMANCE_NAME = ?, GENRE = ?, CAST_INFO = ?, DESCRIPTION = ?, "
				+ "    POSTER_URL = ?, RUNNING_TIME = ?, SHOW_AT = ?, BOOKING_OPEN_AT = ?, CANCEL_DEADLINE_AT = ?, "
				+ "    MAX_TICKETS_PER_MEMBER = ?, VENUE_NAME = ?, VENUE_ADDRESS = ?, APPROVAL_STATUS = ? "
				+ "WHERE PERFORMANCE_ID = ?";
		return executeChange(sql, new Object[] {
				p.getVenueTemplateId(), p.getPerformanceName(), p.getGenre(), p.getCastInfo(),
				p.getDescription(), p.getPosterUrl(), p.getRunningTime(), toTimestamp(p.getShowAt()),
				toTimestamp(p.getBookingOpenAt()), toTimestamp(p.getCancelDeadlineAt()),
				p.getMaxTicketsPerMember(), p.getVenueName(), p.getVenueAddress(), p.getApprovalStatus(),
				p.getPerformanceId() }, "공연 수정 실패");
	}

	/** M10 등급명·가격 수정 */
	public int updateGrade(SeatGrade grade) throws SQLException {
		String sql = "UPDATE SEAT_GRADE SET GRADE_NAME = ?, PRICE = ? WHERE GRADE_ID = ? AND PERFORMANCE_ID = ?";
		return executeChange(sql, new Object[] {
				grade.getGradeName(), grade.getPrice(), grade.getGradeId(), grade.getPerformanceId() },
				"좌석 등급 수정 실패");
	}

	/** M10 대관 증빙 교체 (공연당 1개라 공연 번호로 찾는다) */
	public int updateRentalProof(VenueRentalProof proof) throws SQLException {
		String sql = "UPDATE VENUE_RENTAL_PROOF "
				+ "SET FILE_PATH = ?, ORIGINAL_FILE_NAME = ?, RENTAL_START_AT = ?, RENTAL_END_AT = ?, SUBMITTED_AT = SYSDATE "
				+ "WHERE PERFORMANCE_ID = ?";
		return executeChange(sql, new Object[] {
				proof.getFilePath(), proof.getOriginalFileName(), toTimestamp(proof.getRentalStartAt()),
				toTimestamp(proof.getRentalEndAt()), proof.getPerformanceId() }, "대관 증빙 수정 실패");
	}

	// ===================== M11 삭제 =====================

	/**
	 * M11 공연 삭제: 변경된 행 수를 반환
	 * SEAT_GRADE, VENUE_RENTAL_PROOF는 ON DELETE CASCADE로 함께 지워진다.
	 * 섹터·좌석이 등급을 참조하고 있으면 삭제가 막히므로, Manager에서 좌석·섹터(SeatDAO)를 먼저 지워야 한다.
	 */
	public int delete(long performanceId) throws SQLException {
		return executeChange("DELETE FROM PERFORMANCE WHERE PERFORMANCE_ID = ?",
				new Object[] { performanceId }, "공연 삭제 실패");
	}

	/** M11 예매 내역 존재 여부 (있으면 삭제 불가) */
	public boolean hasBookings(long performanceId) throws SQLException {
		return queryCount("SELECT COUNT(*) FROM BOOKING WHERE PERFORMANCE_ID = ?",
				new Object[] { performanceId }) > 0;
	}

	// ===================== M30 관리자 심사 =====================

	/** M30-1 심사 상태별 공연 목록 (심사 대기 목록은 "P", 먼저 신청한 순) */
	public List<Performance> findListByStatus(String approvalStatus) throws SQLException {
		jdbcUtil.setSqlAndParameters("SELECT * FROM PERFORMANCE WHERE APPROVAL_STATUS = ? ORDER BY CREATED_AT",
				new Object[] { approvalStatus });
		try {
			ResultSet rs = executeQueryOrThrow();
			List<Performance> list = new ArrayList<>();
			while (rs.next()) {
				list.add(mapPerformance(rs));
			}
			return list;
		} finally {
			jdbcUtil.close();
		}
	}

	/** M30-1 심사 상세의 대관 증빙: 없으면 null */
	public VenueRentalProof findRentalProof(long performanceId) throws SQLException {
		jdbcUtil.setSqlAndParameters(
				"SELECT PROOF_ID, PERFORMANCE_ID, FILE_PATH, ORIGINAL_FILE_NAME, RENTAL_START_AT, RENTAL_END_AT, SUBMITTED_AT "
						+ "FROM VENUE_RENTAL_PROOF WHERE PERFORMANCE_ID = ?",
				new Object[] { performanceId });
		try {
			ResultSet rs = executeQueryOrThrow();
			if (!rs.next()) {
				return null;
			}
			VenueRentalProof proof = new VenueRentalProof();
			proof.setProofId(rs.getLong("PROOF_ID"));
			proof.setPerformanceId(rs.getLong("PERFORMANCE_ID"));
			proof.setFilePath(rs.getString("FILE_PATH"));
			proof.setOriginalFileName(rs.getString("ORIGINAL_FILE_NAME"));
			proof.setRentalStartAt(rs.getTimestamp("RENTAL_START_AT"));
			proof.setRentalEndAt(rs.getTimestamp("RENTAL_END_AT"));
			proof.setSubmittedAt(rs.getTimestamp("SUBMITTED_AT"));
			return proof;
		} finally {
			jdbcUtil.close();
		}
	}

	/** M30-2 승인: 심사중(P)인 공연만 바뀐다. 0이 반환되면 이미 심사가 끝난 공연 */
	public int approve(long performanceId, long managerId) throws SQLException {
		String sql = "UPDATE PERFORMANCE "
				+ "SET APPROVAL_STATUS = ?, MANAGER_ID = ?, REJECT_REASON = NULL, REVIEWED_AT = SYSDATE "
				+ "WHERE PERFORMANCE_ID = ? AND APPROVAL_STATUS = ?";
		return executeChange(sql, new Object[] { STATUS_APPROVED, managerId, performanceId, STATUS_PENDING },
				"공연 승인 실패");
	}

	/** M30-3 반려: 심사중(P)인 공연만 바뀐다. 0이 반환되면 이미 심사가 끝난 공연 */
	public int reject(long performanceId, long managerId, String rejectReason) throws SQLException {
		String sql = "UPDATE PERFORMANCE "
				+ "SET APPROVAL_STATUS = ?, MANAGER_ID = ?, REJECT_REASON = ?, REVIEWED_AT = SYSDATE "
				+ "WHERE PERFORMANCE_ID = ? AND APPROVAL_STATUS = ?";
		return executeChange(sql,
				new Object[] { STATUS_REJECTED, managerId, rejectReason, performanceId, STATUS_PENDING },
				"공연 반려 실패");
	}

	// ===================== 내부 공통 처리 =====================

	/** 장르가 있을 때만 조건 추가 */
	private void appendGenre(StringBuilder sql, List<Object> params, String genre) {
		if (genre != null && !genre.isBlank()) {
			sql.append("AND GENRE = ? ");
			params.add(genre);
		}
	}

	/** 검색 조건: 승인된 공연 중 공연명 또는 출연진에 검색어 포함 (+ 장르) */
	private void appendSearchCondition(StringBuilder sql, List<Object> params, String keyword, String genre) {
		String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
		sql.append("WHERE APPROVAL_STATUS = ? AND (PERFORMANCE_NAME LIKE ? OR CAST_INFO LIKE ?) ");
		params.add(STATUS_APPROVED);
		params.add(like);
		params.add(like);
		appendGenre(sql, params, genre);
	}

	/** ROWNUM 페이징 공통 (공연 일시 순) */
	private List<Performance> findPage(String where, List<Object> params, int page, int size) throws SQLException {
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page와 size는 1 이상이어야 합니다.");
		}
		String sql = "SELECT * FROM ( "
				+ "  SELECT ROWNUM AS RN, t.* FROM ( "
				+ "    SELECT * FROM PERFORMANCE " + where + "ORDER BY SHOW_AT "
				+ "  ) t WHERE ROWNUM <= ? "
				+ ") WHERE RN > ?";
		params.add(page * size);        // 이 페이지의 마지막 행 번호
		params.add((page - 1) * size);  // 이전 페이지까지의 행 수

		jdbcUtil.setSqlAndParameters(sql, params.toArray());
		try {
			ResultSet rs = executeQueryOrThrow();
			List<Performance> list = new ArrayList<>();
			while (rs.next()) {
				list.add(mapPerformance(rs));
			}
			return list;
		} finally {
			jdbcUtil.close();
		}
	}

	private Performance mapPerformance(ResultSet rs) throws SQLException {
		Performance p = new Performance();
		p.setPerformanceId(rs.getLong("PERFORMANCE_ID"));
		p.setRegistrantId(rs.getLong("REGISTRANT_ID"));
		p.setManagerId(getNullableLong(rs, "MANAGER_ID"));
		p.setVenueTemplateId(getNullableLong(rs, "VENUE_TEMPLATE_ID"));
		p.setPerformanceName(rs.getString("PERFORMANCE_NAME"));
		p.setGenre(rs.getString("GENRE"));
		p.setCastInfo(rs.getString("CAST_INFO"));
		p.setDescription(rs.getString("DESCRIPTION"));
		p.setPosterUrl(rs.getString("POSTER_URL"));
		p.setRunningTime(rs.getInt("RUNNING_TIME"));
		p.setShowAt(rs.getTimestamp("SHOW_AT"));
		p.setBookingOpenAt(rs.getTimestamp("BOOKING_OPEN_AT"));
		p.setCancelDeadlineAt(rs.getTimestamp("CANCEL_DEADLINE_AT"));
		p.setMaxTicketsPerMember(rs.getInt("MAX_TICKETS_PER_MEMBER"));
		p.setVenueName(rs.getString("VENUE_NAME"));
		p.setVenueAddress(rs.getString("VENUE_ADDRESS"));
		p.setApprovalStatus(rs.getString("APPROVAL_STATUS"));
		p.setRejectReason(rs.getString("REJECT_REASON"));
		p.setReviewedAt(rs.getTimestamp("REVIEWED_AT"));
		p.setCreatedAt(rs.getTimestamp("CREATED_AT"));
		return p;
	}

	/** NULL 가능한 NUMBER 컬럼 (getLong은 NULL을 0으로 돌려주므로 따로 확인) */
	private Long getNullableLong(ResultSet rs, String column) throws SQLException {
		long value = rs.getLong(column);
		return rs.wasNull() ? null : value;
	}

	/** 팀 규칙: 날짜는 java.sql.Timestamp로 변환해서 전달 */
	private Timestamp toTimestamp(Date date) {
		return date == null ? null : new Timestamp(date.getTime());
	}

	private int queryCount(String sql, Object[] params) throws SQLException {
		jdbcUtil.setSqlAndParameters(sql, params);
		try {
			ResultSet rs = executeQueryOrThrow();
			rs.next();
			return rs.getInt(1);
		} finally {
			jdbcUtil.close();
		}
	}

	/** INSERT·UPDATE·DELETE 공통: 성공하면 commit, 실패하면 rollback */
	private int executeChange(String sql, Object[] params, String failMessage) throws SQLException {
		jdbcUtil.setSqlAndParameters(sql, params);
		try {
			int result = jdbcUtil.executeUpdate();
			jdbcUtil.commit();
			return result;
		} catch (Exception ex) {
			rollbackQuietly();
			throw toSQLException(failMessage, ex);
		} finally {
			jdbcUtil.close();
		}
	}

	/** 시퀀스 다음 값 조회 (close는 호출한 쪽에서) */
	private long nextSequence(String sequenceName) throws SQLException {
		jdbcUtil.setSqlAndParameters("SELECT " + sequenceName + ".NEXTVAL FROM DUAL", null);
		ResultSet rs = executeQueryOrThrow();
		if (!rs.next()) {
			throw new SQLException(sequenceName + " 값을 가져오지 못했습니다.");
		}
		return rs.getLong(1);
	}

	/** JDBCUtil.executeQuery()는 실패하면 null을 반환하므로 예외로 바꿔서 알린다 */
	private ResultSet executeQueryOrThrow() throws SQLException {
		ResultSet rs = jdbcUtil.executeQuery();
		if (rs == null) {
			throw new SQLException("쿼리 실행 실패: " + jdbcUtil.getSql());
		}
		return rs;
	}

	/** 연결을 얻기 전에 실패한 경우에도 rollback에서 다른 예외가 나지 않게 막는다 */
	private void rollbackQuietly() {
		try {
			jdbcUtil.rollback();
		} catch (Exception ignore) {
			// 원래 예외를 그대로 전달하기 위해 무시
		}
	}

	private SQLException toSQLException(String message, Exception ex) {
		if (ex instanceof SQLException) {
			return (SQLException) ex;
		}
		return new SQLException(message, ex);
	}
}

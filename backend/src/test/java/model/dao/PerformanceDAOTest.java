package model.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.domain.Performance;
import model.domain.SeatGrade;
import model.domain.VenueRentalProof;
import model.dto.MyPerformanceSummary;

/**
 * PerformanceDAO 테스트
 * 팀 공용 DB를 쓰므로 테스트용 회원·공연은 끝날 때 반드시 지운다.
 * (MemberDAO는 다른 브랜치라서 테스트용 회원은 JDBCUtil로 직접 넣는다)
 */
public class PerformanceDAOTest {
	private static final long DAY = 24L * 60 * 60 * 1000;
	private static final long HOUR = 60L * 60 * 1000;

	private PerformanceDAO performanceDAO;
	private long registrantId = 0;
	private long adminId = 0;
	private long performanceId = 0;
	private String uniqueName;

	@Before
	public void setUp() throws Exception {
		assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
				getClass().getClassLoader().getResource("db.properties") != null);
		performanceDAO = new PerformanceDAO();

		String suffix = String.valueOf(System.currentTimeMillis() % 100000000L);
		uniqueName = "테스트공연_" + suffix;
		registrantId = insertTestMember("t_reg_" + suffix, "USER");
		adminId = insertTestMember("t_adm_" + suffix, "ADMIN");
	}

	@After
	public void tearDown() throws Exception {
		// 등급·증빙은 공연 삭제 시 CASCADE로 같이 지워진다
		if (performanceId != 0) {
			executeSql("DELETE FROM PERFORMANCE WHERE PERFORMANCE_ID = ?", performanceId);
		}
		if (registrantId != 0) {
			executeSql("DELETE FROM MEMBER WHERE MEMBER_ID = ?", registrantId);
		}
		if (adminId != 0) {
			executeSql("DELETE FROM MEMBER WHERE MEMBER_ID = ?", adminId);
		}
	}

	@Test
	public void createFindAndReview() throws Exception {
		Performance p = newPerformance();
		performanceId = performanceDAO.create(p);
		assertTrue(performanceId > 0);

		// 상세 조회: 심사중(P)으로 저장됐는지
		Performance found = performanceDAO.findById(performanceId);
		assertNotNull(found);
		assertEquals(uniqueName, found.getPerformanceName());
		assertEquals(Performance.STATUS_PENDING, found.getApprovalStatus());
		assertNull(found.getManagerId());
		assertNull(found.getVenueTemplateId());
		assertNotNull(found.getShowAt());

		// 등급 2개: 비싼 순으로 조회
		performanceDAO.createGrade(new SeatGrade(performanceId, "R", 120000));
		performanceDAO.createGrade(new SeatGrade(performanceId, "VIP", 150000));
		List<SeatGrade> grades = performanceDAO.findGradeList(performanceId);
		assertEquals(2, grades.size());
		assertEquals("VIP", grades.get(0).getGradeName());

		// 대관 증빙
		VenueRentalProof proof = new VenueRentalProof();
		proof.setPerformanceId(performanceId);
		proof.setFilePath("/upload/proof/test.pdf");
		proof.setOriginalFileName("대관확인서.pdf");
		proof.setRentalStartAt(new Date(p.getShowAt().getTime() - 2 * HOUR));
		proof.setRentalEndAt(new Date(p.getShowAt().getTime() + 5 * HOUR));
		assertTrue(performanceDAO.createRentalProof(proof) > 0);

		VenueRentalProof foundProof = performanceDAO.findRentalProof(performanceId);
		assertNotNull(foundProof);
		assertTrue(foundProof.covers(found.getShowAt()));

		// 심사 대기 목록에 있어야 함
		assertTrue(containsId(performanceDAO.findListByStatus(Performance.STATUS_PENDING), performanceId));

		// 승인 전에는 목록·검색에 안 보임
		assertFalse(containsId(performanceDAO.search(uniqueName, null, 1, 10), performanceId));

		// 승인: 처음엔 1, 두 번째는 이미 처리돼서 0
		assertEquals(1, performanceDAO.approve(performanceId, adminId));
		assertEquals(0, performanceDAO.approve(performanceId, adminId));
		assertEquals(0, performanceDAO.reject(performanceId, adminId, "사유"));

		Performance approved = performanceDAO.findById(performanceId);
		assertEquals(Performance.STATUS_APPROVED, approved.getApprovalStatus());
		assertEquals(Long.valueOf(adminId), approved.getManagerId());
		assertNotNull(approved.getReviewedAt());

		// 승인 후에는 검색·장르 목록에 보임
		assertTrue(containsId(performanceDAO.search(uniqueName, null, 1, 10), performanceId));
		assertTrue(containsId(performanceDAO.search(uniqueName, "CONCERT", 1, 10), performanceId));
		assertFalse(containsId(performanceDAO.search(uniqueName, "MUSICAL", 1, 10), performanceId));
		assertEquals(1, performanceDAO.countSearch(uniqueName, null));
		assertTrue(performanceDAO.countApproved("CONCERT") >= 1);
		assertTrue(performanceDAO.countApproved(null) >= 1);
	}

	@Test
	public void rejectPendingPerformance() throws Exception {
		performanceId = performanceDAO.create(newPerformance());

		assertEquals(1, performanceDAO.reject(performanceId, adminId, "대관 기간이 공연 일시와 다릅니다"));
		Performance rejected = performanceDAO.findById(performanceId);
		assertEquals(Performance.STATUS_REJECTED, rejected.getApprovalStatus());
		assertEquals("대관 기간이 공연 일시와 다릅니다", rejected.getRejectReason());
	}

	@Test
	public void findMyListAndUpdate() throws Exception {
		Performance p = newPerformance();
		performanceId = performanceDAO.create(p);

		// 좌석을 아직 안 만들었으므로 전체·잔여 모두 0
		List<MyPerformanceSummary> mine = performanceDAO.findMyList(registrantId);
		assertEquals(1, mine.size());
		assertEquals(performanceId, mine.get(0).getPerformance().getPerformanceId());
		assertEquals(0, mine.get(0).getTotalSeats());
		assertEquals(0, mine.get(0).getAvailableSeats());

		// 공연 정보 수정
		p.setPerformanceName(uniqueName + "_수정");
		p.setRunningTime(150);
		p.setApprovalStatus(Performance.STATUS_PENDING);
		assertEquals(1, performanceDAO.update(p));
		Performance updated = performanceDAO.findById(performanceId);
		assertEquals(uniqueName + "_수정", updated.getPerformanceName());
		assertEquals(150, updated.getRunningTime());

		// 등급 수정
		SeatGrade grade = new SeatGrade(performanceId, "S", 90000);
		performanceDAO.createGrade(grade);
		grade.setPrice(95000);
		assertEquals(1, performanceDAO.updateGrade(grade));
		assertEquals(95000, performanceDAO.findGradeList(performanceId).get(0).getPrice());

		// 예매 내역 없음
		assertFalse(performanceDAO.hasBookings(performanceId));
	}

	@Test
	public void deletePerformance() throws Exception {
		performanceId = performanceDAO.create(newPerformance());
		performanceDAO.createGrade(new SeatGrade(performanceId, "R", 100000));

		assertEquals(1, performanceDAO.delete(performanceId));
		assertNull(performanceDAO.findById(performanceId));
		assertTrue(performanceDAO.findGradeList(performanceId).isEmpty()); // CASCADE 확인
		performanceId = 0; // 이미 지웠으므로 tearDown에서 건너뜀
	}

	// ===================== 테스트 데이터 =====================

	/** CHECK 제약에 맞는 공연: 예매 오픈 < 공연 일시, 취소 기한 <= 공연 일시 */
	private Performance newPerformance() {
		long now = System.currentTimeMillis();
		Performance p = new Performance();
		p.setRegistrantId(registrantId);
		p.setPerformanceName(uniqueName);
		p.setGenre("CONCERT");
		p.setCastInfo("테스트밴드");
		p.setDescription("DAO 테스트용 공연");
		p.setRunningTime(120);
		p.setShowAt(new Date(now + 30 * DAY));
		p.setBookingOpenAt(new Date(now + 1 * DAY));
		p.setCancelDeadlineAt(new Date(now + 29 * DAY));
		p.setMaxTicketsPerMember(4);
		p.setVenueName("테스트홀");
		p.setVenueAddress("서울시 테스트구");
		return p;
	}

	private long insertTestMember(String loginId, String role) throws Exception {
		JDBCUtil jdbcUtil = new JDBCUtil();
		try {
			jdbcUtil.setSqlAndParameters("SELECT SEQ_MEMBER.NEXTVAL FROM DUAL", null);
			ResultSet rs = jdbcUtil.executeQuery();
			assertNotNull("쿼리 실행 실패 — db.properties의 접속 정보를 확인하세요", rs);
			rs.next();
			long memberId = rs.getLong(1);

			jdbcUtil.setSqlAndParameters(
					"INSERT INTO MEMBER (MEMBER_ID, LOGIN_ID, PASSWORD_HASH, MEMBER_NAME, EMAIL, MEMBER_ROLE, CREATED_AT) "
							+ "VALUES (?, ?, 'test_hash', '테스트', ?, ?, SYSDATE)",
					new Object[] { memberId, loginId, loginId + "@test.com", role });
			jdbcUtil.executeUpdate();
			jdbcUtil.commit();
			return memberId;
		} finally {
			jdbcUtil.close();
		}
	}

	private void executeSql(String sql, long id) throws Exception {
		JDBCUtil jdbcUtil = new JDBCUtil();
		try {
			jdbcUtil.setSqlAndParameters(sql, new Object[] { id });
			jdbcUtil.executeUpdate();
			jdbcUtil.commit();
		} finally {
			jdbcUtil.close();
		}
	}

	private boolean containsId(List<Performance> list, long id) {
		for (Performance p : list) {
			if (p.getPerformanceId() == id) {
				return true;
			}
		}
		return false;
	}
}

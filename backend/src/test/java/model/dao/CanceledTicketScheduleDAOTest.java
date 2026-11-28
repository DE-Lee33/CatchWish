package model.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import model.domain.CanceledTicketSchedule;
import model.domain.Performance;

public class CanceledTicketScheduleDAOTest {
    private static final long DAY = 24L * 60 * 60 * 1000;

    private long memberId;
    private long performanceId;

    @Before
    public void setUp() throws Exception {
        assumeTrue("db.properties를 만들어주세요",
                getClass().getClassLoader().getResource("db.properties") != null);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        insertTestMember("t_reopen_" + suffix);

        long now = System.currentTimeMillis();
        Performance performance = new Performance();
        performance.setRegistrantId(memberId);
        performance.setPerformanceName("재오픈 테스트_" + suffix);
        performance.setGenre("CONCERT");
        performance.setCastInfo("테스트밴드");
        performance.setDescription("취소표 재오픈 DAO 테스트");
        performance.setRunningTime(120);
        performance.setShowAt(new Date(now + 30 * DAY));
        performance.setBookingOpenAt(new Date(now + DAY));
        performance.setCancelDeadlineAt(new Date(now + 29 * DAY));
        performance.setMaxTicketsPerMember(4);
        performance.setVenueName("테스트홀");
        performance.setVenueAddress("서울시 테스트구");

        performanceId = new PerformanceDAO().create(performance);
        assertTrue("테스트 공연 등록 실패", performanceId > 0);
    }

    @Test
    public void insertScheduleSavesPendingSchedule() throws Exception {
        Timestamp openAt = Timestamp.valueOf(
                LocalDateTime.now().plusDays(2).withSecond(0).withNano(0));

        CanceledTicketSchedule schedule = new CanceledTicketSchedule();
        schedule.setPerformanceId(performanceId);
        schedule.setOpenAt(openAt);

        assertEquals(1, new CanceledTicketScheduleDAO().insertSchedule(schedule));

        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters(
                    "SELECT PERFORMANCE_ID, OPEN_AT, SCHEDULE_STATUS "
                            + "FROM CANCELED_TICKET_SCHEDULE WHERE PERFORMANCE_ID = ?",
                    new Object[] { performanceId });

            try (ResultSet rs = jdbc.executeQuery()) {
                assertNotNull("재오픈 일정 조회 실패", rs);
                assertTrue("저장된 일정이 없습니다", rs.next());
                assertEquals(performanceId, rs.getLong("PERFORMANCE_ID"));
                assertEquals(openAt, rs.getTimestamp("OPEN_AT"));
                assertEquals("P", rs.getString("SCHEDULE_STATUS"));
                assertFalse("일정이 두 건 이상 저장됐습니다", rs.next());
            }
        } finally {
            jdbc.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (performanceId > 0) {
            deleteTestData(
                    "DELETE FROM CANCELED_TICKET_SCHEDULE WHERE PERFORMANCE_ID = ?",
                    performanceId);
            deleteTestData(
                    "DELETE FROM PERFORMANCE WHERE PERFORMANCE_ID = ?",
                    performanceId);
        }

        if (memberId > 0) {
            deleteTestData("DELETE FROM MEMBER WHERE MEMBER_ID = ?", memberId);
        }
    }

    private void insertTestMember(String loginId) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters("SELECT SEQ_MEMBER.NEXTVAL FROM DUAL", null);

            try (ResultSet rs = jdbc.executeQuery()) {
                assertNotNull("회원 ID 조회 실패", rs);
                assertTrue(rs.next());
                memberId = rs.getLong(1);
            }

            jdbc.setSqlAndParameters(
                    "INSERT INTO MEMBER "
                            + "(MEMBER_ID, LOGIN_ID, PASSWORD_HASH, MEMBER_NAME, "
                            + "EMAIL, MEMBER_ROLE, CREATED_AT) "
                            + "VALUES (?, ?, 'test_hash', '테스트', ?, 'USER', SYSDATE)",
                    new Object[] { memberId, loginId, loginId + "@test.com" });

            assertEquals(1, jdbc.executeUpdate());
            jdbc.commit();
        } finally {
            jdbc.close();
        }
    }

    private void deleteTestData(String sql, long id) throws Exception {
        JDBCUtil jdbc = new JDBCUtil();
        try {
            jdbc.setSqlAndParameters(sql, new Object[] { id });
            jdbc.executeUpdate();
            jdbc.commit();
        } finally {
            jdbc.close();
        }
    }
}


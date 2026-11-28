package controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import common.auth.LoginCheckInterceptor;
import common.auth.LoginMember;
import common.auth.LoginSession;
import common.error.GlobalExceptionHandler;
import model.manager.CanceledTicketScheduleManager;

public class CanceledTicketScheduleControllerTest {
    private static final String URL = "/api/performances/{performanceId}/canceled-ticket-schedules";
    private static final String REQUEST_BODY = "{\"openAt\":\"2030-01-02T19:00\"}";

    private RecordingScheduleManager manager;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        manager = new RecordingScheduleManager();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CanceledTicketScheduleController(manager))
                .addInterceptors(new LoginCheckInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void passesPerformanceIdOpenAtAndSessionMemberIdToManager() throws Exception {
        MockHttpServletRequest loginRequest = new MockHttpServletRequest();
        LoginSession.login(loginRequest,
                new LoginMember(7L, "test_host", "테스트 주최자", "USER"));
        MockHttpSession session = (MockHttpSession) loginRequest.getSession(false);

        mockMvc.perform(post(URL, 42L)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
                .andExpect(status().isNoContent());

        assertTrue(manager.called);
        assertEquals(42L, manager.performanceId);
        assertEquals(7L, manager.loginMemberId);

        // 한국 시간으로 해석됐는지 확인한다.
        Timestamp expectedOpenAt = Timestamp.from(Instant.parse("2030-01-02T10:00:00Z"));
        assertEquals(expectedOpenAt, manager.openAt);
    }

    @Test
    public void rejectsRequestWithoutLogin() throws Exception {
        mockMvc.perform(post(URL, 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
                .andExpect(status().isUnauthorized());

        assertFalse(manager.called);
    }

    private static class RecordingScheduleManager extends CanceledTicketScheduleManager {
        private boolean called;
        private long performanceId;
        private long loginMemberId;
        private Timestamp openAt;

        RecordingScheduleManager() {
            super(null, null);
        }

        @Override
        public void registerSchedule(long performanceId, Timestamp openAt, long loginMemberId) {
            this.called = true;
            this.performanceId = performanceId;
            this.openAt = openAt;
            this.loginMemberId = loginMemberId;
        }
    }
}
package controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import common.auth.LoginCheckInterceptor;
import common.auth.LoginMember;
import common.auth.LoginSession;
import common.error.ApiException;
import common.error.ErrorCode;
import common.error.GlobalExceptionHandler;
import model.domain.Performance;
import model.dto.PerformanceRegisterRequest;
import model.manager.PerformanceManager;

public class PerformanceControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private StubManager manager;

    /** Manager를 대신하는 가짜: 받은 값을 기록하고, 원하면 예외를 던진다 */
    static class StubManager extends PerformanceManager {
        boolean called;
        long lastMemberId;
        boolean receivedPoster;
        RuntimeException toThrow;
        String genre;
        int page;

        StubManager() { super(null, null, null); }

        @Override
        public long registerPerformance(long registrantId, PerformanceRegisterRequest req,
                MultipartFile proof, MultipartFile poster) {
            called = true;
            lastMemberId = registrantId;
            receivedPoster = (poster != null);
            if (toThrow != null) throw toThrow;
            return 77L;
        }

        // 목록 HTTP 처리만 확인하려고 받은 값을 기억하고 결과를 고정한다 (페이지 규칙은 PerformanceManagerTest에서 확인)
        @Override
        public List<Performance> findPerformanceList(String genre, int page) {
            this.genre = genre;
            this.page = page;
            Performance performance = new Performance();
            performance.setPerformanceId(7L);
            performance.setPerformanceName("2026 캐치위시 페스티벌");
            return List.of(performance);
        }

        @Override
        public int countPerformances(String genre) {
            return 23;
        }
    }

    @Before
    public void setUp() {
        manager = new StubManager();
        mockMvc = MockMvcBuilders.standaloneSetup(new PerformanceController(manager))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new LoginCheckInterceptor())
                .build();
    }

    private MockHttpSession login() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginSession.login(request, new LoginMember(42L, "tester", "테스터", "USER"));
        return (MockHttpSession) request.getSession();
    }

    private MockMultipartFile data() {
        return new MockMultipartFile("data", "", "application/json", "{\"performanceName\":\"테스트\"}".getBytes());
    }
    private MockMultipartFile proof() {
        return new MockMultipartFile("proof", "대관.pdf", "application/pdf", "pdf".getBytes());
    }
    private MockMultipartFile poster() {
        return new MockMultipartFile("poster", "p.png", "image/png", "img".getBytes());
    }

    /** 한 줄 요약 출력: 테스트이름 | 보낸 것 | 응답 | 결과 */
    private void print(String name, String sent, MvcResult r, String check) throws Exception {
        System.out.println("\n========== ✔ " + name + " ==========");
        System.out.println("보낸 것 : " + sent);
        System.out.println("응답    : " + r.getResponse().getStatus() + " " + r.getResponse().getContentAsString());
        System.out.println("확인    : " + check);
        System.out.println("=====================================\n");
    }

    @Test
    public void 비로그인이면_401() throws Exception {
        MvcResult r = mockMvc.perform(multipart("/api/performances").file(data()).file(proof()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("\"code\":\"AUTH_REQUIRED\"")))
                .andReturn();
        assertFalse(manager.called);
        print("비로그인 → 401", "로그인 없이 data + proof", r, "Manager 호출 안 됨");
    }

    @Test
    public void 로그인하면_201이고_세션_회원번호가_Manager로_전달된다() throws Exception {
        MvcResult r = mockMvc.perform(multipart("/api/performances").file(data()).file(proof()).file(poster())
                .session(login()))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"performanceId\":77")))
                .andReturn();
        assertEquals(42L, manager.lastMemberId);
        assertTrue(manager.receivedPoster);
        print("로그인 → 201", "세션 회원 42 + data + proof + poster", r,
                "Manager가 받은 memberId=" + manager.lastMemberId + " (세션 값), 포스터 전달됨");
    }

    @Test
    public void 포스터_없이도_201() throws Exception {
        MvcResult r = mockMvc.perform(multipart("/api/performances").file(data()).file(proof()).session(login()))
                .andExpect(status().isCreated())
                .andReturn();
        assertFalse(manager.receivedPoster);
        print("포스터 없음 → 201", "data + proof (poster 생략)", r, "Manager가 받은 poster=null");
    }

    @Test
    public void proof_파트가_없으면_400() throws Exception {
        MvcResult r = mockMvc.perform(multipart("/api/performances").file(data()).session(login()))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertFalse(manager.called);
        print("proof 누락 → 400", "data만", r, "Manager 호출 안 됨");
    }

    @Test
    public void data_파트가_없으면_400() throws Exception {
        MvcResult r = mockMvc.perform(multipart("/api/performances").file(proof()).session(login()))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertFalse(manager.called);
        print("data 누락 → 400", "proof만", r, "Manager 호출 안 됨");
    }

    @Test
    public void Manager가_ApiException을_던지면_code와_message가_내려간다() throws Exception {
        manager.toThrow = new ApiException(ErrorCode.BAD_REQUEST, "공연명은 1~200자여야 합니다.");
        MvcResult r = mockMvc.perform(multipart("/api/performances").file(data()).file(proof()).session(login()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"code\":\"BAD_REQUEST\"")))
                .andExpect(content().string(containsString("공연명은 1~200자여야 합니다.")))
                .andReturn();
        print("Manager 오류 → 400", "Manager가 BAD_REQUEST 던지도록 설정", r, "code·message가 {code, message} 형식으로 내려감");
    }
    
    @Test
    public void 용량_초과_예외면_413_FILE_TOO_LARGE() throws Exception {
        manager.toThrow = new MaxUploadSizeExceededException(10 * 1024 * 1024);
        MvcResult r = mockMvc.perform(multipart("/api/performances").file(data()).file(proof()).session(login()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().string(containsString("\"code\":\"FILE_TOO_LARGE\"")))
                .andReturn();
        print("용량 초과 → 413", "Manager가 MaxUploadSizeExceededException 던지도록 설정", r, "code=FILE_TOO_LARGE");
    }

    @Test
    public void 목록과_전체_개수를_응답한다() throws Exception {
        String json = mockMvc.perform(get("/api/performances").param("genre", "CONCERT").param("page", "2"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(json);
        assertEquals(23, body.get("totalCount").asInt());
        assertEquals(7L, body.get("performanceList").get(0).get("performanceId").asLong());
        assertEquals("CONCERT", manager.genre);
        assertEquals(2, manager.page);
    }

    @Test
    public void 파라미터가_없으면_전체_1페이지() throws Exception {
        mockMvc.perform(get("/api/performances")).andExpect(status().isOk());

        assertNull(manager.genre);
        assertEquals(1, manager.page);
    }

    @Test
    public void 페이지가_숫자가_아니면_400() throws Exception {
        mockMvc.perform(get("/api/performances").param("page", "abc")).andExpect(status().isBadRequest());
    }
}

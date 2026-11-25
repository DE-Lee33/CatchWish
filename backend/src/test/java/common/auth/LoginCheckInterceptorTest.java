package common.auth;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import common.error.GlobalExceptionHandler;

public class LoginCheckInterceptorTest {
    private MockMvc mockMvc;

    @RestController
    static class DummyController {
        @GetMapping("/api/test/public")
        public String open() { return "ok"; }

        @LoginRequired
        @GetMapping("/api/test/member")
        public String member() { return "ok"; }

        @LoginRequired(adminOnly = true)
        @GetMapping("/api/test/admin")
        public String admin() { return "ok"; }
    }

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new LoginCheckInterceptor())
                .build();
    }

    /** LoginSession.login()으로 로그인된 세션을 만든다 */
    private MockHttpSession sessionOf(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginSession.login(request, new LoginMember(1L, "tester", "테스터", role));
        return (MockHttpSession) request.getSession();
    }

    @Test
    public void 어노테이션_없는_API는_로그인_없이_통과() throws Exception {
        mockMvc.perform(get("/api/test/public")).andExpect(status().isOk());
    }

    @Test
    public void 로그인_필요_API는_비로그인시_401() throws Exception {
        mockMvc.perform(get("/api/test/member"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("\"code\":\"AUTH_REQUIRED\"")));
    }

    @Test
    public void 로그인_필요_API는_로그인시_통과() throws Exception {
        mockMvc.perform(get("/api/test/member").session(sessionOf("USER")))
                .andExpect(status().isOk());
    }

    @Test
    public void 관리자_전용_API는_일반회원이면_403() throws Exception {
        mockMvc.perform(get("/api/test/admin").session(sessionOf("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("\"code\":\"FORBIDDEN\"")));
    }

    @Test
    public void 관리자_전용_API는_관리자면_통과() throws Exception {
        mockMvc.perform(get("/api/test/admin").session(sessionOf("ADMIN")))
                .andExpect(status().isOk());
    }
}
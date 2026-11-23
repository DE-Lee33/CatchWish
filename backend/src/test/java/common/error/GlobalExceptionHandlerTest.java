package common.error;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

public class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    /** 테스트 전용 컨트롤러 (운영 코드에 포함되지 않음) */
    @RestController
    static class DummyController {
        @GetMapping("/test/conflict")
        public String conflict() { throw new ApiException(ErrorCode.CONFLICT); }

        @GetMapping("/test/boom")
        public String boom() { throw new IllegalStateException("secret-detail"); }

        @PostMapping("/test/body")
        public String body(@RequestBody Map<String, Object> body) { return "ok"; }
    }

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void apiException은_해당_상태코드와_code로_응답한다() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("\"code\":\"CONFLICT\"")));
    }

    @Test
    public void 깨진_JSON은_400() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"code\":\"BAD_REQUEST\"")));
    }

    @Test
    public void 허용되지_않은_메서드는_405() throws Exception {
        mockMvc.perform(post("/test/conflict"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().string(containsString("\"code\":\"METHOD_NOT_ALLOWED\"")));
    }

    @Test
    public void 예상못한_예외는_500이고_내부_메시지를_노출하지_않는다() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("\"code\":\"INTERNAL_ERROR\"")))
                .andExpect(content().string(not(containsString("secret-detail"))));
    }
}
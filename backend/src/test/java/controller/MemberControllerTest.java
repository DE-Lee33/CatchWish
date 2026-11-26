package controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import common.auth.LoginMember;
import common.auth.LoginSession;
import common.error.GlobalExceptionHandler;

public class MemberControllerTest {
	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockMvc mockMvc;

	@Before
	public void setUp() {
		// 로그아웃 후 /me까지 이어서 확인하려고 AuthController도 같이 등록 (로그아웃은 DB를 쓰지 않아 Manager가 없어도 된다)
		mockMvc = MockMvcBuilders.standaloneSetup(new MemberController(), new AuthController(null))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	private MockHttpSession loggedInSession() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		LoginSession.login(request, new LoginMember(5L, "hyunji01", "현지", "USER"));
		return (MockHttpSession) request.getSession();
	}

	@Test
	public void 로그인했으면_세션의_회원정보를_돌려준다() throws Exception {
		String json = mockMvc.perform(get("/api/members/me").session(loggedInSession()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode body = objectMapper.readTree(json);
		assertEquals(5L, body.get("memberId").asLong());
		assertEquals("hyunji01", body.get("loginId").asText());
		assertEquals("현지", body.get("memberName").asText());
	}

	@Test
	public void 로그인_안_했으면_401_AUTH_REQUIRED() throws Exception {
		String json = mockMvc.perform(get("/api/members/me"))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertEquals("AUTH_REQUIRED", objectMapper.readTree(json).get("code").asText());
	}

	@Test
	public void 로그아웃하면_다음_요청부터_401() throws Exception {
		MockHttpSession session = loggedInSession();
		mockMvc.perform(get("/api/members/me").session(session)).andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/members/me").session(session)).andExpect(status().isUnauthorized());
	}
}

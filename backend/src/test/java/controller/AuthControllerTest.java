package controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import common.auth.LoginMember;
import common.auth.LoginSession;
import common.error.ApiException;
import common.error.ErrorCode;
import common.error.GlobalExceptionHandler;
import model.domain.Member;
import model.manager.MemberManager;

public class AuthControllerTest {
	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockMvc mockMvc;

	// HTTP 처리만 확인하려고 로그인 결과를 고정한 가짜 Manager (검증 규칙은 MemberManagerTest에서 확인)
	private static class FakeMemberManager extends MemberManager {
		FakeMemberManager() {
			super(null);
		}

		@Override
		public Member login(String loginId, String password) {
			if (!"hyunji01".equals(loginId) || !"test1234".equals(password)) {
				throw new ApiException(ErrorCode.LOGIN_FAILED);
			}
			Member member = new Member("hyunji01", "hash_should_not_leak", "현지", "hyunji01@test.com");
			member.setMemberId(5L);
			return member;
		}
	}

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(new FakeMemberManager()))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	public void 로그인_성공하면_회원정보를_응답하고_세션에_저장한다() throws Exception {
		MvcResult result = login("hyunji01", "test1234").andExpect(status().isOk()).andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		assertEquals(5L, body.get("memberId").asLong());
		assertEquals("hyunji01", body.get("loginId").asText());
		assertEquals("현지", body.get("memberName").asText());
		assertEquals("USER", body.get("role").asText());
		assertFalse(result.getResponse().getContentAsString().contains("hash_should_not_leak"));

		LoginMember saved = LoginSession.get(result.getRequest());
		assertEquals(5L, saved.memberId());
	}

	@Test
	public void 로그인_실패하면_401_LOGIN_FAILED이고_세션이_없다() throws Exception {
		MvcResult result = login("hyunji01", "test9999").andExpect(status().isUnauthorized()).andReturn();

		assertErrorCode(result.getResponse(), "LOGIN_FAILED");
		assertNull(LoginSession.get(result.getRequest()));
	}

	@Test
	public void 아이디나_비밀번호가_비어_있으면_400() throws Exception {
		assertErrorCode(login(" ", "test1234").andExpect(status().isBadRequest()).andReturn().getResponse(), "BAD_REQUEST");
		assertErrorCode(login("hyunji01", "").andExpect(status().isBadRequest()).andReturn().getResponse(), "BAD_REQUEST");
	}

	@Test
	public void 요청_본문이_없으면_400() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void 로그아웃하면_204이고_세션이_사라진다() throws Exception {
		MockHttpSession session = (MockHttpSession) login("hyunji01", "test1234")
				.andReturn().getRequest().getSession(false);

		mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
		assertTrue(session.isInvalid());
	}

	@Test
	public void 로그인하지_않고_로그아웃해도_204() throws Exception {
		mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
	}

	private ResultActions login(String loginId, String password) throws Exception {
		String json = objectMapper.writeValueAsString(Map.of("loginId", loginId, "password", password));
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private void assertErrorCode(MockHttpServletResponse response, String code) throws Exception {
		assertEquals(code, objectMapper.readTree(response.getContentAsString()).get("code").asText());
	}
}

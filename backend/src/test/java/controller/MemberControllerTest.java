package controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import common.auth.LoginMember;
import common.auth.LoginSession;
import common.error.ApiException;
import common.error.ErrorCode;
import common.error.GlobalExceptionHandler;
import model.manager.MemberManager;

public class MemberControllerTest {
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final FakeMemberManager memberManager = new FakeMemberManager();
	private MockMvc mockMvc;

	// HTTP 처리만 확인하려고 가입 결과를 고정한 가짜 Manager (검증 규칙은 MemberManagerTest에서 확인)
	private static class FakeMemberManager extends MemberManager {
		private String receivedMemberName;
		private String receivedEmail;

		FakeMemberManager() {
			super(null);
		}

		@Override
		public long create(String loginId, String password, String memberName, String email) {
			if ("hyunji01".equals(loginId)) {
				throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID);
			}
			receivedMemberName = memberName;
			receivedEmail = email;
			return 101;
		}

		@Override
		public boolean existsLoginId(String loginId) {
			return "hyunji01".equals(loginId);
		}
	}

	@Before
	public void setUp() {
		// 로그아웃 후 /me까지 이어서 확인하려고 AuthController도 같이 등록 (로그아웃은 DB를 쓰지 않아 Manager가 없어도 된다)
		mockMvc = MockMvcBuilders.standaloneSetup(new MemberController(memberManager), new AuthController(null))
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

	@Test
	public void 가입하면_201과_회원번호를_돌려준다() throws Exception {
		String json = signup("newbie01").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		assertEquals(101L, objectMapper.readTree(json).get("memberId").asLong());
		assertEquals("새회원", memberManager.receivedMemberName);
		assertEquals("newbie@test.com", memberManager.receivedEmail);
	}

	@Test
	public void 가입_오류는_Manager의_오류코드로_응답한다() throws Exception {
		String json = signup("hyunji01").andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();

		assertEquals("DUPLICATE_LOGIN_ID", objectMapper.readTree(json).get("code").asText());
	}

	@Test
	public void 가입_요청_본문이_없으면_400() throws Exception {
		mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	private ResultActions signup(String loginId) throws Exception {
		String json = objectMapper.writeValueAsString(Map.of("loginId", loginId, "password", "password1",
				"memberName", "새회원", "email", "newbie@test.com"));
		return mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	public void 중복확인은_사용_가능_여부를_available로_응답한다() throws Exception {
		assertEquals(true, checkLoginId("newbie01").get("available").asBoolean());
		assertEquals(false, checkLoginId("hyunji01").get("available").asBoolean());
	}

	@Test
	public void 중복확인에_loginId가_없으면_400() throws Exception {
		mockMvc.perform(get("/api/members/check-id")).andExpect(status().isBadRequest());
	}

	private JsonNode checkLoginId(String loginId) throws Exception {
		String json = mockMvc.perform(get("/api/members/check-id").param("loginId", loginId))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json);
	}
}

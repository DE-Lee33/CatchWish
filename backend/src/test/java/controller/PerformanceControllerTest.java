package controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import common.error.GlobalExceptionHandler;
import model.domain.Performance;
import model.manager.PerformanceManager;

public class PerformanceControllerTest {
	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockMvc mockMvc;
	private FakePerformanceManager performanceManager;

	// HTTP 처리만 확인하려고 받은 값을 기억하고 결과를 고정한 가짜 Manager (페이지 규칙은 PerformanceManagerTest에서 확인)
	private static class FakePerformanceManager extends PerformanceManager {
		String genre;
		int page;

		FakePerformanceManager() {
			super(null);
		}

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
		performanceManager = new FakePerformanceManager();
		mockMvc = MockMvcBuilders.standaloneSetup(new PerformanceController(performanceManager))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	public void 목록과_전체_개수를_응답한다() throws Exception {
		String json = mockMvc.perform(get("/api/performances").param("genre", "CONCERT").param("page", "2"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		JsonNode body = objectMapper.readTree(json);
		assertEquals(23, body.get("totalCount").asInt());
		assertEquals(7L, body.get("performanceList").get(0).get("performanceId").asLong());
		assertEquals("CONCERT", performanceManager.genre);
		assertEquals(2, performanceManager.page);
	}

	@Test
	public void 파라미터가_없으면_전체_1페이지() throws Exception {
		mockMvc.perform(get("/api/performances")).andExpect(status().isOk());

		assertNull(performanceManager.genre);
		assertEquals(1, performanceManager.page);
	}

	@Test
	public void 페이지가_숫자가_아니면_400() throws Exception {
		mockMvc.perform(get("/api/performances").param("page", "abc")).andExpect(status().isBadRequest());
	}
}

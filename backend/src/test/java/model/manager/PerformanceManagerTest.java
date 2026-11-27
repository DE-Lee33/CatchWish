package model.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Test;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.PerformanceDAO;
import model.domain.Performance;

public class PerformanceManagerTest {

	// DB 없이 Manager가 DAO에 무엇을 넘기는지만 확인하려고 받은 값을 기억하는 가짜 DAO
	private static class FakePerformanceDAO extends PerformanceDAO {
		String genre;
		int page;
		int size;

		@Override
		public List<Performance> findApprovedList(String genre, int page, int size) {
			this.genre = genre;
			this.page = page;
			this.size = size;
			return List.of();
		}
	}

	private final FakePerformanceDAO performanceDAO = new FakePerformanceDAO();
	private final PerformanceManager performanceManager = new PerformanceManager(performanceDAO);

	@Test
	public void 장르와_페이지를_한_페이지_10개로_DAO에_넘긴다() throws Exception {
		performanceManager.findPerformanceList("CONCERT", 2);

		assertEquals("CONCERT", performanceDAO.genre);
		assertEquals(2, performanceDAO.page);
		assertEquals(10, performanceDAO.size);
	}

	@Test
	public void 페이지가_1보다_작으면_BAD_REQUEST() {
		ApiException ex = assertThrows(ApiException.class, () -> performanceManager.findPerformanceList(null, 0));
		assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
	}
}

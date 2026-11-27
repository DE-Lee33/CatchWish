package model.manager;

import java.util.List;

import org.springframework.stereotype.Service;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.PerformanceDAO;
import model.domain.Performance;

@Service
public class PerformanceManager {
	// 화면이 한 페이지 개수를 정하면 요청마다 다르게 보낼 수 있어 서버가 고정한다
	static final int PAGE_SIZE = 10;

	private final PerformanceDAO performanceDAO;

	public PerformanceManager(PerformanceDAO performanceDAO) {
		this.performanceDAO = performanceDAO;
	}

	public List<Performance> findPerformanceList(String genre, int page) throws Exception {
		// DAO는 page가 1보다 작으면 IllegalArgumentException(500)을 던지므로 그 전에 400으로 막는다
		if (page < 1) {
			throw new ApiException(ErrorCode.BAD_REQUEST);
		}
		return performanceDAO.findApprovedList(genre, page, PAGE_SIZE);
	}

	public int countPerformances(String genre) throws Exception {
		return performanceDAO.countApproved(genre);
	}
}

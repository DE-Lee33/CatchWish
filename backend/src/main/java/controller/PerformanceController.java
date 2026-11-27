package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import model.dto.PerformanceListResponse;
import model.manager.PerformanceManager;

@RestController
@RequestMapping("/api/performances")
public class PerformanceController {
	private final PerformanceManager performanceManager;

	public PerformanceController(PerformanceManager performanceManager) {
		this.performanceManager = performanceManager;
	}

	// 메인 화면이라 로그인 없이 부른다. genre가 없으면 전체
	@GetMapping
	public PerformanceListResponse list(@RequestParam(required = false) String genre,
			@RequestParam(defaultValue = "1") int page) throws Exception {
		return new PerformanceListResponse(
				performanceManager.findPerformanceList(genre, page),
				performanceManager.countPerformances(genre));
	}
}

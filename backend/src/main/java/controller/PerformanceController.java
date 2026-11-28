package controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import common.auth.LoginRequired;
import common.auth.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import model.dto.PerformanceRegisterRequest;
import model.manager.PerformanceManager;

@RestController
@RequestMapping("/api/performances")
public class PerformanceController {
    private final PerformanceManager performanceManager;

    public PerformanceController(PerformanceManager performanceManager) {
        this.performanceManager = performanceManager;
    }

    @LoginRequired
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> register(
            @RequestPart("data") PerformanceRegisterRequest req,
            @RequestPart("proof") MultipartFile proof,
            @RequestPart(value = "poster", required = false) MultipartFile poster,
            HttpServletRequest request) throws Exception {
        long memberId = LoginSession.require(request).memberId();
        long id = performanceManager.registerPerformance(memberId, req, proof, poster);
        return ResponseEntity.status(201).body(Map.of("performanceId", id));
    }
}
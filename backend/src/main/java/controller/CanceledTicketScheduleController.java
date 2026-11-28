package controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import common.auth.LoginRequired;
import common.auth.LoginSession;
import common.error.ApiException;
import common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import model.manager.CanceledTicketScheduleManager;

@RestController
public class CanceledTicketScheduleController {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final CanceledTicketScheduleManager scheduleManager;

    public CanceledTicketScheduleController(CanceledTicketScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
    }

    // 취소표 재오픈 일정 등록 (M27)
    @LoginRequired
    @PostMapping("/api/performances/{performanceId}/canceled-ticket-schedules")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerSchedule(
            @PathVariable("performanceId") long performanceId,
            @RequestBody RegisterScheduleRequest body,
            HttpServletRequest request) throws Exception {
        long loginMemberId = LoginSession.require(request).memberId();

        Timestamp openAt = null;
        if (body.openAt() != null && !body.openAt().isBlank()) {
            try {
                // 화면에서 선택한 날짜와 시간을 한국 시간으로 해석한다.
                LocalDateTime localOpenAt = LocalDateTime.parse(body.openAt());
                openAt = Timestamp.from(localOpenAt.atZone(SEOUL_ZONE).toInstant());
            } catch (DateTimeParseException e) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "재오픈 날짜와 시간을 다시 선택해 주세요.");
            }
        }

        scheduleManager.registerSchedule(performanceId, openAt, loginMemberId);
    }

    public record RegisterScheduleRequest(String openAt) {}
}
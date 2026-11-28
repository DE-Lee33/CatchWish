package controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import common.auth.LoginMember;
import common.auth.LoginSession;
import common.error.ApiException;
import common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import model.dto.BookingHoldResponse;
import model.dto.HoldRequest;
import model.manager.BookingManager;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingManager bookingManager;

    public BookingController(BookingManager bookingManager) {
        this.bookingManager = bookingManager;
    }

    @PostMapping("/hold")
    public BookingHoldResponse hold(@RequestBody HoldRequest req, HttpServletRequest request) {
        LoginMember me = LoginSession.require(request);          // 비로그인이면 401
        if (req.performanceId() == null) throw new ApiException(ErrorCode.BAD_REQUEST);
        return bookingManager.hold(me.memberId(), req.performanceId(), req.seatIds());
    }
}
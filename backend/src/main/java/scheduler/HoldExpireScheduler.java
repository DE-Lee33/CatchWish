package scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import model.manager.BookingManager;

@Component
public class HoldExpireScheduler {
    private static final Logger log = LoggerFactory.getLogger(HoldExpireScheduler.class);
    private final BookingManager bookingManager;

    public HoldExpireScheduler(BookingManager bookingManager) {
        this.bookingManager = bookingManager;
    }

    /** 이전 실행이 끝난 뒤 30초 후 재실행 (겹쳐 실행되지 않음) */
    @Scheduled(fixedDelay = 30_000)
    public void expireHolds() {
        try {
            int n = bookingManager.expireHolds();
            if (n > 0) log.info("홀드 만료 처리: {}건", n);
        } catch (Exception e) {
            log.error("홀드 만료 스케줄러 오류", e);
        }
    }
}
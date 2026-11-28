package model.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import common.error.ApiException;
import common.error.ErrorCode;
import model.dao.BookingDAO;
import model.dao.BookingSeatDAO;
import model.dao.JDBCUtil;
import model.dto.BookingHoldResponse;

/**
 * 실제 DB(테스트용)에 붙는 통합 테스트.
 * 각 테스트는 try/finally 안에서 실행되어, 성공/실패와 상관없이 데이터를 원상복구한다.
 */
public class BookingManagerTest {

    // ★ 테스트 DB에 실제 있는 값으로 수정
    static final long MEMBER_A = 239, MEMBER_B = 240; // A: signuptest01, B: jaewon0922
    static final long PERFORMANCE_ID = 113;
    static final List<Long> SEATS = Arrays.asList(367L, 368L, 369L);   // 이 공연의 좌석, 모두 'A' 상태

    private final BookingManager bookingManager =
            new BookingManager(new BookingDAO(), new SeatManager(new BookingSeatDAO()));

    // ---------------------------------------------------------------- 선점

    @Test
    public void 선점하면_좌석이_H가_되고_만료시각이_약_10분_뒤다() throws Exception {
        run(() -> {
            BookingHoldResponse res = bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));

            assertTrue(res.remainingSeconds() > 590 && res.remainingSeconds() <= 600);
            assertEquals("H", seatStatus(SEATS.get(0)));
            assertEquals("W", bookingStatus(res.bookingId()));

            int minutes = Integer.parseInt(query(
                    "SELECT ROUND((HOLD_EXPIRES_AT - SYSDATE) * 1440) FROM BOOKING WHERE BOOKING_ID=" + res.bookingId()));
            assertTrue(minutes >= 9 && minutes <= 10);
        });
    }

    @Test
    public void 같은_좌석을_다른_회원이_선점하면_SEAT_ALREADY_HELD() throws Exception {
        run(() -> {
            bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));

            assertHoldFails(ErrorCode.SEAT_ALREADY_HELD, MEMBER_B, Arrays.asList(SEATS.get(0)));
            assertEquals("H", seatStatus(SEATS.get(0)));
        });
    }

    @Test
    public void 여러_좌석_중_하나라도_실패하면_전체_롤백() throws Exception {
        run(() -> {
            bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(1)));

            assertHoldFails(ErrorCode.SEAT_ALREADY_HELD, MEMBER_B, Arrays.asList(SEATS.get(0), SEATS.get(1)));

            assertEquals("A", seatStatus(SEATS.get(0)));      // 101도 선점되면 안 됨
            assertEquals("0", query("SELECT COUNT(*) FROM BOOKING WHERE MEMBER_ID=" + MEMBER_B
                    + " AND PERFORMANCE_ID=" + PERFORMANCE_ID + " AND BOOKING_STATUS='W'"));
        });
    }

    @Test
    public void 같은_회원이_다시_선택하면_기존_홀드가_해제된다() throws Exception {
        run(() -> {
            BookingHoldResponse first = bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));
            bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(1)));

            assertEquals("A", seatStatus(SEATS.get(0)));
            assertEquals("H", seatStatus(SEATS.get(1)));
            assertEquals("X", bookingStatus(first.bookingId()));
        });
    }

    @Test
    public void 입력이_잘못되면_BAD_REQUEST() throws Exception {
        run(() -> {
            assertHoldFails(ErrorCode.BAD_REQUEST, MEMBER_A, null);
            assertHoldFails(ErrorCode.BAD_REQUEST, MEMBER_A, Arrays.<Long>asList());
            assertHoldFails(ErrorCode.BAD_REQUEST, MEMBER_A, Arrays.asList(SEATS.get(0), SEATS.get(0)));
        });
    }

    // ---------------------------------------------------------------- 동시성

    @Test
    public void 동시에_같은_좌석을_요청하면_정확히_한_명만_성공한다() throws Exception {
        run(() -> {
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger ok = new AtomicInteger();
            AtomicInteger conflict = new AtomicInteger();

            for (long member : new long[] { MEMBER_A, MEMBER_B }) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                        bookingManager.hold(member, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));
                        ok.incrementAndGet();
                    } catch (ApiException e) {
                        if (e.getErrorCode() == ErrorCode.SEAT_ALREADY_HELD) {
                            conflict.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {
                    }
                });
            }

            ready.await();
            go.countDown();                                   // 두 스레드를 동시에 출발
            pool.shutdown();
            assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));

            assertEquals(1, ok.get());
            assertEquals(1, conflict.get());
            assertEquals("H", seatStatus(SEATS.get(0)));
        });
    }

    // ---------------------------------------------------------------- 매수 제한

    @Test
    public void 최대_매수를_넘기면_MAX_TICKETS_EXCEEDED() throws Exception {
        run(() -> {
            String original = query("SELECT NVL(TO_CHAR(MAX_TICKETS_PER_MEMBER),'NULL') FROM PERFORMANCE WHERE PERFORMANCE_ID="
                    + PERFORMANCE_ID);
            try {
                update("UPDATE PERFORMANCE SET MAX_TICKETS_PER_MEMBER=2 WHERE PERFORMANCE_ID=" + PERFORMANCE_ID);

                assertHoldFails(ErrorCode.MAX_TICKETS_EXCEEDED, MEMBER_A, SEATS);   // 3매 요청
                assertEquals("A", seatStatus(SEATS.get(0)));
            } finally {
                update("UPDATE PERFORMANCE SET MAX_TICKETS_PER_MEMBER=" + ("NULL".equals(original) ? "NULL" : original) + " WHERE PERFORMANCE_ID=" + PERFORMANCE_ID);
            }
        });
    }

    // ---------------------------------------------------------------- 홀드 만료

    @Test
    public void 만료된_홀드는_예매가_X가_되고_좌석이_A로_복원된다() throws Exception {
        run(() -> {
            BookingHoldResponse res = bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));
            update("UPDATE BOOKING SET HOLD_EXPIRES_AT = SYSDATE - 1 WHERE BOOKING_ID = " + res.bookingId());

            assertTrue(bookingManager.expireHolds() >= 1);

            assertEquals("A", seatStatus(SEATS.get(0)));
            assertEquals("X", bookingStatus(res.bookingId()));
        });
    }

    @Test
    public void 아직_만료되지_않은_홀드는_건드리지_않는다() throws Exception {
        run(() -> {
            BookingHoldResponse res = bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));

            bookingManager.expireHolds();

            assertEquals("H", seatStatus(SEATS.get(0)));
            assertEquals("W", bookingStatus(res.bookingId()));
        });
    }

    @Test
    public void 만료_뒤에는_다른_회원이_그_좌석을_선점할_수_있다() throws Exception {
        run(() -> {
            BookingHoldResponse res = bookingManager.hold(MEMBER_A, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));
            update("UPDATE BOOKING SET HOLD_EXPIRES_AT = SYSDATE - 1 WHERE BOOKING_ID = " + res.bookingId());
            bookingManager.expireHolds();

            BookingHoldResponse second = bookingManager.hold(MEMBER_B, PERFORMANCE_ID, Arrays.asList(SEATS.get(0)));

            assertFalse(second.bookingId() == res.bookingId());
            assertEquals("H", seatStatus(SEATS.get(0)));
        });
    }

    // ---------------------------------------------------------------- 도우미

    /** 테스트 전에 한 번 정리하고, 끝나면(실패해도) 다시 정리한다. @Before/@After 대신 사용 */
    private void run(ThrowingRunnable body) throws Exception {
        cleanup();
        try {
            body.run();
        } catch (Exception | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            cleanup();
        }
    }

    /** 테스트 회원이 이 공연에서 만든 W/X 예매를 지우고, 테스트 좌석을 A로 되돌린다 */
    private void cleanup() throws Exception {
        String mine = "MEMBER_ID IN (" + MEMBER_A + "," + MEMBER_B + ") AND PERFORMANCE_ID = " + PERFORMANCE_ID
                + " AND BOOKING_STATUS IN ('W','X')";
        update("DELETE FROM BOOKING_SEAT WHERE BOOKING_ID IN (SELECT BOOKING_ID FROM BOOKING WHERE " + mine + ")");
        update("DELETE FROM BOOKING WHERE " + mine);
        update("UPDATE SEAT SET SEAT_STATUS='A' WHERE SEAT_ID IN (" + idList(SEATS) + ")");
    }

    private void assertHoldFails(ErrorCode expected, long memberId, List<Long> seatIds) {
        ApiException ex = assertThrows(ApiException.class,
                () -> bookingManager.hold(memberId, PERFORMANCE_ID, seatIds));
        assertEquals(expected, ex.getErrorCode());
    }

    private static String idList(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        return sb.toString();
    }

    private static void update(String sql) throws Exception {
        JDBCUtil j = new JDBCUtil();
        try {
            j.setSqlAndParameters(sql, new Object[] {});
            j.executeUpdate();
            j.commit();
        } finally {
            j.close();
        }
    }

    private static String query(String sql) {
        JDBCUtil j = new JDBCUtil();
        try {
            j.setSqlAndParameters(sql, new Object[] {});
            ResultSet rs = j.executeQuery();
            rs.next();
            return rs.getString(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            j.close();
        }
    }

    private static String seatStatus(long seatId) {
        return query("SELECT SEAT_STATUS FROM SEAT WHERE SEAT_ID=" + seatId);
    }

    private static String bookingStatus(long bookingId) {
        return query("SELECT BOOKING_STATUS FROM BOOKING WHERE BOOKING_ID=" + bookingId);
    }
}
package model.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import common.error.ApiException;
import common.error.ErrorCode;
import controller.config.WebConfig;
import model.dao.JDBCUtil;
import model.dto.WishlistResponseDTO;

/**
 * 실제 DAO·DB로 Manager 규칙을 확인하는 통합 테스트.
 * 전제: db/seed.sql(seed_user2, '2026 캐치위시 페스티벌', 재원님 구역 데이터)이 들어 있어야 한다.
 * db.properties가 없거나 seed_user2의 위시가 비어 있지 않으면 건너뛴다(남의 데이터를 지우지 않기 위해).
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = WebConfig.class)
public class WishlistManagerIntegrationTest {
    private static final String UNIQUE_CONSTRAINT = "UK_WISHLIST_MBR_PERF_SECTOR";

    @Autowired
    private WishlistManager manager;

    private Long memberId;
    private Long performanceId;
    private List<Long> sectorIds;
    private boolean ready = false; // 전제 조건을 통과한 경우에만 정리(cleanUp)를 수행

    @BeforeClass
    public static void requireDbProperties() {
        assumeNotNull(WishlistManagerIntegrationTest.class.getClassLoader().getResource("db.properties"));
    }

    @Before
    public void setUp() throws Exception {
        memberId = queryFirst("SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = ?", "seed_user2");
        performanceId = queryFirst("SELECT PERFORMANCE_ID FROM PERFORMANCE WHERE PERFORMANCE_NAME = ?",
                "2026 캐치위시 페스티벌");
        assumeNotNull(memberId, performanceId);
        assumeTrue("seed_user2에 기존 위시가 있어 건너뜀", manager.findWishlistList(memberId).isEmpty());
        sectorIds = queryLongs("SELECT SECTOR_ID FROM SECTOR WHERE PERFORMANCE_ID = ? ORDER BY SECTOR_ID",
                performanceId);
        ready = true;
    }

    @After
    public void cleanUp() throws Exception {
        if (!ready) return;
        for (WishlistResponseDTO item : manager.findWishlistList(memberId)) {
            manager.remove(item.getWishlist().getWishlistId(), memberId);
        }
    }

    private static void assertErrorCode(ErrorCode expected, ThrowingRunnable action) {
        ApiException ex = assertThrows(ApiException.class, action);
        assertEquals(expected, ex.getErrorCode());
    }

    @Test
    public void 등록하면_방금_등록한_항목을_돌려준다() throws Exception {
        WishlistResponseDTO created = manager.create(memberId, performanceId, null, 2);
        assertEquals(performanceId, created.getWishlist().getPerformanceId());
        assertEquals(Integer.valueOf(2), created.getWishlist().getPriority());
    }

    @Test
    public void 같은_공연_구역을_다시_등록하면_CONFLICT() throws Exception {
        manager.create(memberId, performanceId, null, null);
        assertErrorCode(ErrorCode.CONFLICT, () -> manager.create(memberId, performanceId, null, null));
    }

    @Test
    public void 같은_공연이라도_구역이_다르면_등록된다() throws Exception {
        assumeTrue("공연에 구역이 2개 이상 필요", sectorIds.size() >= 2);
        manager.create(memberId, performanceId, sectorIds.get(0), null);
        manager.create(memberId, performanceId, sectorIds.get(1), null);
        assertEquals(2, manager.findWishlistList(memberId).size());
    }

    @Test
    public void 다른_공연의_구역이면_NOT_FOUND() throws Exception {
        Long otherSector = queryFirst("SELECT MAX(SECTOR_ID) FROM SECTOR WHERE PERFORMANCE_ID <> ?", performanceId);
        assumeNotNull(otherSector);
        assertErrorCode(ErrorCode.NOT_FOUND, () -> manager.create(memberId, performanceId, otherSector, null));
    }

    @Test
    public void 없는_공연이면_NOT_FOUND() {
        // 실제 DAO·DB의 ORA-02291이 NOT_FOUND로 바뀌는지 확인
        assertErrorCode(ErrorCode.NOT_FOUND, () -> manager.create(memberId, -1L, null, null));
    }

    @Test
    public void 공연이_없으면_BAD_REQUEST() {
        assertErrorCode(ErrorCode.BAD_REQUEST, () -> manager.create(memberId, null, null, null));
    }

    @Test
    public void 우선순위가_1보다_작으면_BAD_REQUEST() {
        assertErrorCode(ErrorCode.BAD_REQUEST, () -> manager.create(memberId, performanceId, null, 0));
    }

    @Test
    public void 삭제할_항목이_없으면_NOT_FOUND() {
        assertErrorCode(ErrorCode.NOT_FOUND, () -> manager.remove(-1L, memberId));
    }

    @Test
    public void 우선순위를_일괄_변경하면_요청한_순서대로_바뀐다() throws Exception {
        assumeTrue("공연에 구역이 1개 이상 필요", !sectorIds.isEmpty());
        Long first = manager.create(memberId, performanceId, null, 1).getWishlist().getWishlistId();
        Long second = manager.create(memberId, performanceId, sectorIds.get(0), 2).getWishlist().getWishlistId();

        List<WishlistResponseDTO> result = manager.reorder(memberId, List.of(second, first));

        assertEquals(second, result.get(0).getWishlist().getWishlistId());
        assertEquals(Integer.valueOf(1), result.get(0).getWishlist().getPriority());
        assertEquals(first, result.get(1).getWishlist().getWishlistId());
        assertEquals(Integer.valueOf(2), result.get(1).getWishlist().getPriority());
    }

    @Test
    public void 일괄_변경_목록이_현재_목록과_다르면_CONFLICT() throws Exception {
        manager.create(memberId, performanceId, null, null);
        assertErrorCode(ErrorCode.CONFLICT, () -> manager.reorder(memberId, List.of(-1L)));
    }

    @Test
    public void 일괄_변경_목록이_비어_있으면_BAD_REQUEST() {
        assertErrorCode(ErrorCode.BAD_REQUEST, () -> manager.reorder(memberId, List.of()));
    }

    @Test
    public void 동시에_같은_항목을_등록해도_한_건만_저장된다() throws Exception {
        assumeTrue("유니크 제약(db/migrations) 반영 후 실행",
                Long.valueOf(1).equals(queryFirst(
                        "SELECT COUNT(*) FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME = ?", UNIQUE_CONSTRAINT)));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ErrorCode>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Callable<ErrorCode> task = () -> {
                start.await();
                try {
                    manager.create(memberId, performanceId, null, null);
                    return null;
                } catch (ApiException e) {
                    return e.getErrorCode();
                }
            };
            results.add(pool.submit(task));
        }
        start.countDown();

        int conflicts = 0;
        for (Future<ErrorCode> result : results) {
            ErrorCode code = result.get(15, TimeUnit.SECONDS);
            if (code != null) {
                assertEquals(ErrorCode.CONFLICT, code);
                conflicts++;
            }
        }
        pool.shutdown();

        assertEquals(1, conflicts);
        assertEquals(1, manager.findWishlistList(memberId).size());
    }

    // ---- 테스트용 조회 헬퍼 (JDBCUtil 사용 순서 그대로) ----
    private static List<Long> queryLongs(String sql, Object... params) throws Exception {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            jdbcUtil.setSqlAndParameters(sql, params);
            ResultSet rs = jdbcUtil.executeQuery();
            List<Long> values = new ArrayList<>();
            while (rs.next()) {
                long value = rs.getLong(1);
                values.add(rs.wasNull() ? null : value);
            }
            return values;
        } finally {
            jdbcUtil.close();
        }
    }

    private static Long queryFirst(String sql, Object... params) throws Exception {
        List<Long> values = queryLongs(sql, params);
        return values.isEmpty() ? null : values.get(0);
    }
}

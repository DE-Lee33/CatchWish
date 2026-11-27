package model.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.sql.ResultSet;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import controller.config.MyBatisConfig;
import model.domain.Wishlist;
import model.dto.WishlistResponseDTO;

public class WishlistDAOTest {

    private static AnnotationConfigApplicationContext ctx;
    private WishlistDAO wishlistDAO;

    @BeforeClass
    public static void initContext() {
        Assume.assumeTrue("db.properties가 없어 건너뜀",
            WishlistDAOTest.class.getClassLoader().getResource("db.properties") != null);
        // WishlistDAO를 빈으로 등록해 생성자 주입(WishlistMapper)까지 함께 확인
        ctx = new AnnotationConfigApplicationContext(MyBatisConfig.class, WishlistDAO.class);
    }

    @AfterClass
    public static void closeContext() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Before
    public void setUp() {
        wishlistDAO = ctx.getBean(WishlistDAO.class);
    }

    // 회원의 위시리스트 조회 테스트 (없는 회원 → 빈 리스트)
    @Test
    public void testSelectWishlistByMemberId() throws Exception {
        List<WishlistResponseDTO> result =
                wishlistDAO.selectWishlistByMemberId(Long.MAX_VALUE);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // 중첩 매핑 확인 (seed의 seed_user1 위시리스트)
    @Test
    public void seed_회원의_위시리스트가_중첩_매핑된다() throws Exception {
        Long memberId = findMemberId("seed_user1");
        Assume.assumeNotNull(memberId);

        List<WishlistResponseDTO> result = wishlistDAO.selectWishlistByMemberId(memberId);
        Assume.assumeFalse("위시 seed 데이터가 없어 건너뜀", result.isEmpty());

        WishlistResponseDTO dto = result.get(0);
        assertNotNull(dto.getWishlist());
        assertNotNull(dto.getWishlist().getWishlistId());
        assertEquals(memberId, dto.getWishlist().getMemberId());
        assertNotNull(dto.getPerformanceTitle());
        
        // 정렬: PRIORITY 오름차순, NULL은 맨 뒤
        boolean seenNull = false;
        Integer prev = null;
        for (WishlistResponseDTO r : result) {
            Integer p = r.getWishlist().getPriority();
            if (p == null) {
                seenNull = true;
            } else {
                assertFalse("NULL priority 뒤에 값이 올 수 없습니다", seenNull);
                if (prev != null) {
                    assertTrue(prev <= p);
                }
                prev = p;
            }
        }
    }

    // 위시리스트 삭제 테스트
    @Test
    public void testDeleteWishlist() throws Exception {
        int result = wishlistDAO.deleteWishlist(Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(0, result);
    }

    // 위시리스트 우선순위 변경 테스트
    @Test
    public void testUpdatePriority() throws Exception {
        int result = wishlistDAO.updatePriority(Long.MAX_VALUE, Long.MAX_VALUE, 1);
        assertEquals(0, result);
    }

    // NULL 처리 테스트
    @Test
    public void testWishlistNullPriority() {
        Wishlist wishlist = new Wishlist(1L, 1L, 1L, 1L, null, null);
        assertNull(wishlist.getPriority());
    }

    private Long findMemberId(String loginId) throws Exception {
        JDBCUtil jdbcUtil = new JDBCUtil();
        try {
            jdbcUtil.setSqlAndParameters(
                "SELECT MEMBER_ID FROM MEMBER WHERE LOGIN_ID = ?",
                new Object[] { loginId });
            ResultSet rs = jdbcUtil.executeQuery();
            if (rs != null && rs.next()) {
                return rs.getLong(1);
            }
            return null;
        } finally {
            jdbcUtil.close();
        }
    }
}

package model.dao;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import model.dto.VenueResponseDTO;

/**
 * VenueDAO 테스트 클래스
 */
public class VenueDAOTest {

    private VenueDAO venueDao;

    @Before
    public void setUp() {
        assumeTrue("db.properties를 만들어주세요 (src/main/resources/db.properties.example 참고)",
                getClass().getClassLoader().getResource("db.properties") != null);
        venueDao = new VenueDAO();
        System.out.println("[SETUP] VenueDAO 생성 완료, db.properties 확인됨");
    }

    @Test
    public void testSelectVenueList() {
        System.out.println("\n[TEST] selectVenueList 시작");
        try {
            List<VenueResponseDTO> venueList = venueDao.selectVenueList();

            assertNotNull("공연장 목록이 null이 아니어야 합니다.", venueList);
            System.out.println("[PASS] 목록이 null이 아님 (총 " + venueList.size() + "개)");

            if (venueList.isEmpty()) {
                System.out.println("[INFO] VENUE 테이블에 데이터가 없습니다. 쿼리는 정상 실행됨");
                return;
            }

            for (VenueResponseDTO venue : venueList) {
                System.out.println("  ID: " + venue.getVenueId()
                        + ", 이름: " + venue.getVenueName()
                        + ", 주소: " + venue.getVenueAddress()
                        + ", 수용인원: " + venue.getVenueCapacity());

                assertNotNull("공연장 이름은 null이 아니어야 합니다.", venue.getVenueName());
            }
            System.out.println("[PASS] 모든 공연장의 이름이 null이 아님");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }

    @Test
    public void testSelectVenueById() {
        System.out.println("\n[TEST] selectVenueById 시작");
        try {
            // 하드코딩된 1L 대신 실제 존재하는 첫 번째 공연장 ID 사용
            List<VenueResponseDTO> venueList = venueDao.selectVenueList();
            assumeTrue("VENUE 데이터가 필요합니다.", venueList != null && !venueList.isEmpty());

            long testVenueId = venueList.get(0).getVenueId();
            System.out.println("[INFO] 조회할 ID: " + testVenueId);

            VenueResponseDTO venue = venueDao.selectVenueById(testVenueId);

            assertNotNull("존재하는 ID인데 null이 반환되었습니다.", venue);
            System.out.println("[PASS] 조회 성공 - ID: " + venue.getVenueId()
                    + ", 이름: " + venue.getVenueName()
                    + ", 주소: " + venue.getVenueAddress()
                    + ", 수용인원: " + venue.getVenueCapacity());

            assertEquals("조회된 ID가 일치해야 합니다.", testVenueId, venue.getVenueId());
            System.out.println("[PASS] 요청 ID와 조회된 ID 일치");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }

    @Test
    public void testSelectVenueByIdNotFound() {
        System.out.println("\n[TEST] selectVenueById (없는 ID) 시작");
        try {
            long notExistingId = Long.MAX_VALUE;
            VenueResponseDTO venue = venueDao.selectVenueById(notExistingId);

            assertNull("존재하지 않는 ID는 null이어야 합니다.", venue);
            System.out.println("[PASS] 없는 ID(" + notExistingId + ")는 null 반환");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }
}
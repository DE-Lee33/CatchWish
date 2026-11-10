package model.dao;

import static org.junit.Assert.*;

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
        venueDao = new VenueDAO();
    }

    @Test
    public void testSelectVenueList() {
        try {
            List<VenueResponseDTO> venueList = venueDao.selectVenueList();
            
            // 목록이 null이 아니며 정상적으로 조회되는지 확인
            assertNotNull("공연장 목록이 null이 아니어야 합니다.", venueList);
            
            // 데이터가 존재한다면 내용 출력 및 검증
            System.out.println("=== 공연장 목록 조회 결과 (총 " + venueList.size() + "개) ===" );
            for (VenueResponseDTO venue : venueList) {
                System.out.println("ID: " + venue.getVenueId() 
                        + ", 이름: " + venue.getVenueName() 
                        + ", 주소: " + venue.getVenueAddress() 
                        + ", 수용인원: " + venue.getVenueCapacity());
                
                assertNotNull("공연장 이름은 null이 아니어야 합니다.", venue.getVenueName());
            }
        } catch (SQLException e) {
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }

    @Test
    public void testSelectVenueById() {
        long testVenueId = 1L; // 실제 테스트 DB에 존재하는 공연장 ID로 변경해서 테스트하세요.
        
        try {
            VenueResponseDTO venue = venueDao.selectVenueById(testVenueId);
            
            if (venue != null) {
                System.out.println("=== 특정 공연장 상세 조회 결과 ===");
                System.out.println("ID: " + venue.getVenueId() + ", 이름: " + venue.getVenueName());
                
                assertEquals("조회된 ID가 일치해야 합니다.", testVenueId, venue.getVenueId());
            } else {
                System.out.println("해당 ID(" + testVenueId + ")를 가진 공연장이 존재하지 않습니다.");
            }
        } catch (SQLException e) {
            fail("SQL 예외 발생: " + e.getMessage());
        }
    }
}
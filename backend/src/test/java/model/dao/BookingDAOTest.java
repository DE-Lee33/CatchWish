
package model.dao;

import java.util.List;

import model.dto.BookingResponse;
import model.dto.BookingResponseDTO;

public class BookingDAOTest {

    private static final BookingDAO bookingDAO = new BookingDAO();

    public static void main(String[] args) {

        System.out.println("===== BookingDAO 테스트 시작 =====");

        try {
            testSelectBookingsByMemberId();
            testSelectBookingDetail();

            System.out.println("===== BookingDAO 테스트 종료 =====");

        } catch (Exception e) {
            System.out.println("테스트 실패");
            e.printStackTrace();
        }
    }

    /**
     * M16 - 예매 내역 조회
     * 존재하지 않는 회원 ID로 조회하여 SQL 실행 여부 확인
     */
    public static void testSelectBookingsByMemberId()
            throws Exception {

        System.out.println("\n[테스트 1] 예매 내역 조회");

        List<BookingResponseDTO> bookingList =
                bookingDAO.selectBookingsByMemberId(-1L);

        if (bookingList != null && bookingList.isEmpty()) {
            System.out.println("성공: SQL 정상 실행, 조회 결과 0건");
        } else {
            System.out.println("조회 결과: " + bookingList);
        }
    }

    /**
     * M16 - 예매 상세 조회
     * 존재하지 않는 예매 ID로 조회하여 SQL 실행 여부 확인
     */
    public static void testSelectBookingDetail()
            throws Exception {

        System.out.println("\n[테스트 2] 예매 상세 조회");

        BookingResponse booking =
                bookingDAO.selectBookingDetail(-1L, -1L);

        if (booking == null) {
            System.out.println("성공: SQL 정상 실행, 조회 결과 없음");
        } else {
            System.out.println("조회 결과: " + booking.getBookingId());
        }
    }
}

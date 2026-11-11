package model.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.domain.Seat;
import model.dto.SectorAvailabilityDTO;

public class SeatDAO {
    private JDBCUtil jdbcUtil = null;

    public SeatDAO() {
        jdbcUtil = new JDBCUtil();
    }

    //특정 좌석 배치도의 전체 좌석 조회
    public List<Seat> selectSeatsByLayoutId(long seatLayoutId)
            throws SQLException {

        String sql = "SELECT seat_id, seat_layout_id, sector_id, "
                   + "row_num, column_num, seat_number, seat_status "
                   + "FROM seat "
                   + "WHERE seat_layout_id = ? "
                   + "ORDER BY sector_id, row_num, column_num";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId });

        List<Seat> seatList = new ArrayList<>();

        try {
            ResultSet rs = jdbcUtil.executeQuery();

            if (rs == null) {
                throw new SQLException("좌석 배치도 조회 실패");
            }

            while (rs.next()) {
                Seat seat = new Seat();

                seat.setSeatId(rs.getLong("seat_id"));
                seat.setSeatLayoutId(rs.getLong("seat_layout_id"));
                seat.setSectorId(rs.getLong("sector_id"));
                seat.setRowNum(rs.getInt("row_num"));
                seat.setColumnNum(rs.getInt("column_num"));
                seat.setSeatNumber(rs.getString("seat_number"));
                seat.setSeatStatus(rs.getString("seat_status"));

                seatList.add(seat);
            }

            return seatList;

        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            }
            throw new SQLException("좌석 배치도 조회 실패", ex);

        } finally {
            jdbcUtil.close();
        }
    }

    // 특정 좌석 배치도의 섹터별 잔여 좌석 조회
    public List<SectorAvailabilityDTO> selectSectorAvailability(long seatLayoutId)
            throws SQLException {

        String sql = "SELECT s.sector_id, s.sector_name, "
                   + "COUNT(st.seat_id) AS total_seats, "
                   + "COUNT(CASE WHEN st.seat_status = 'A' "
                   + "THEN 1 END) AS available_seats "
                   + "FROM sector s "
                   + "LEFT JOIN seat st ON s.sector_id = st.sector_id "
                   + "AND st.seat_layout_id = s.seat_layout_id "
                   + "WHERE s.seat_layout_id = ? "
                   + "GROUP BY s.sector_id, s.sector_name "
                   + "ORDER BY s.sector_id";

        jdbcUtil.setSqlAndParameters(sql, new Object[] { seatLayoutId });

        List<SectorAvailabilityDTO> sectorList = new ArrayList<>();

        try {
            ResultSet rs = jdbcUtil.executeQuery();

            if (rs == null) {
                throw new SQLException("섹터별 잔여 좌석 조회 실패");
            }

            while (rs.next()) {
                SectorAvailabilityDTO dto = new SectorAvailabilityDTO();

                dto.setSectorId(rs.getLong("sector_id"));
                dto.setSectorName(rs.getString("sector_name"));
                dto.setTotalSeats(rs.getInt("total_seats"));
                dto.setAvailableSeats(rs.getInt("available_seats"));

                sectorList.add(dto);
            }

            return sectorList;

        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            }
            throw new SQLException("섹터별 잔여 좌석 조회 실패", ex);

        } finally {
            jdbcUtil.close();
        }
    }
}

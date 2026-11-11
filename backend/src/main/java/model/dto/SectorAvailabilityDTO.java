package model.dto;

public class SectorAvailabilityDTO {
    private long sectorId;
    private String sectorName;
    private int totalSeats;
    private int availableSeats;

    public SectorAvailabilityDTO() {
    }

    public SectorAvailabilityDTO(long sectorId, String sectorName,
                              int totalSeats, int availableSeats) {
        this.sectorId = sectorId;
        this.sectorName = sectorName;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }

    public long getSectorId() {
        return sectorId;
    }

    public void setSectorId(long sectorId) {
        this.sectorId = sectorId;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }
}

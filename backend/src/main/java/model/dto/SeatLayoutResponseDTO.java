package model.dto;

public class SeatLayoutResponseDTO {
    private long seatLayoutId;
    private long venueId;
    private String seatLayoutName;
    private String isTemplate;
    private long sectorId;
    private String sectorName;
    
    public SeatLayoutResponseDTO() {
        
    }
    
    public SeatLayoutResponseDTO(long seatLayoutId, long venueId, String seatLayoutName, String isTemplate, long sectorId, String sectorName) {
        this.seatLayoutId = seatLayoutId;
        this.venueId = venueId;
        this.seatLayoutName = seatLayoutName;
        this.isTemplate = isTemplate;
        this.sectorId = sectorId;
        this.sectorName = sectorName;
    }

    public long getSeatLayoutId() {
        return seatLayoutId;
    }

    public void setSeatLayoutId(long seatLayoutId) {
        this.seatLayoutId = seatLayoutId;
    }

    public long getVenueId() {
        return venueId;
    }

    public void setVenueId(long venueId) {
        this.venueId = venueId;
    }

    public String getSeatLayoutName() {
        return seatLayoutName;
    }

    public void setSeatLayoutName(String seatLayoutName) {
        this.seatLayoutName = seatLayoutName;
    }

    public String getIsTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(String isTemplate) {
        this.isTemplate = isTemplate;
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

    @Override
    public String toString() {
        return "SeatLayoutResponseDTO [seatLayoutId=" + seatLayoutId + ", venueId=" + venueId + ", seatLayoutName="
                + seatLayoutName + ", isTemplate=" + isTemplate + ", sectorId=" + sectorId + ", sectorName="
                + sectorName + "]";
    }
}

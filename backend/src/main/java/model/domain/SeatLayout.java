package model.domain;

public class SeatLayout {
    private long seatLayoutId;
    private long venueId;
    private String seatLayoutName;
    private String isTemplate;
    
    public SeatLayout() {
        
    }

    public SeatLayout(long seatLayoutId, long venueId, String seatLayoutName, String isTemplate) {
        super();
        this.seatLayoutId = seatLayoutId;
        this.venueId = venueId;
        this.seatLayoutName = seatLayoutName;
        this.isTemplate = isTemplate;
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

    @Override
    public String toString() {
        return "SeatLayout [seatLayoutId=" + seatLayoutId + ", venueId=" + venueId + ", seatLayoutName="
                + seatLayoutName + ", isTemplate=" + isTemplate + "]";
    }
}

package model.domain;

public class SeatLayout {
    private long seatLayoutId;
    private String seatLayoutName;
    private String isTemplate;
    private long venueId;
    private long sourceLayoutId; 
    
    public SeatLayout() {
    }

    public SeatLayout(long seatLayoutId, String seatLayoutName, String isTemplate, long venueId, long sourceLayoutId) {
        super();
        this.seatLayoutId = seatLayoutId;
        this.seatLayoutName = seatLayoutName;
        this.isTemplate = isTemplate;
        this.venueId = venueId;
        this.sourceLayoutId = sourceLayoutId;
    }

    public long getSeatLayoutId() {
        return seatLayoutId;
    }

    public void setSeatLayoutId(long seatLayoutId) {
        this.seatLayoutId = seatLayoutId;
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

    public long getVenueId() {
        return venueId;
    }

    public void setVenueId(long venueId) {
        this.venueId = venueId;
    }

    public long getSourceLayoutId() {
        return sourceLayoutId;
    }

    public void setSourceLayoutId(long sourceLayoutId) {
        this.sourceLayoutId = sourceLayoutId;
    }

    @Override
    public String toString() {
        return "SeatLayout [seatLayoutId=" + seatLayoutId + ", seatLayoutName=" + seatLayoutName 
                + ", isTemplate=" + isTemplate + ", venueId=" + venueId + ", sourceLayoutId=" + sourceLayoutId + "]";
    }
}
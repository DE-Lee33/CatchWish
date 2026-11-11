package model.domain;

public class Venue {
    private long venueId;
    private String venueName;
    private String venueAddress;
    private int venueCapacity;
    
    public Venue() {
        
    }
    
    public Venue(long venueId, String venueName, String venueAddress, int venueCapacity) {
        this.venueId = venueId;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.venueCapacity = venueCapacity;
    }

    public long getVenueId() {
        return venueId;
    }

    public void setVenueId(long venueId) {
        this.venueId = venueId;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getVenueAddress() {
        return venueAddress;
    }

    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
    }

    public int getVenueCapacity() {
        return venueCapacity;
    }

    public void setVenueCapacity(int venueCapacity) {
        this.venueCapacity = venueCapacity;
    }
    
    @Override
    public String toString() {
        return "Venue [venueId=" + venueId + ", venueName=" + venueName 
                + ", venueAddress=" + venueAddress + ", venueCapacity=" + venueCapacity + "]";
    }
}

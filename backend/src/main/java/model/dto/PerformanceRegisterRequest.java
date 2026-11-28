package model.dto;

import java.util.Date;
import java.util.List;

public class PerformanceRegisterRequest {
    private Long venueTemplateId; // 템플릿 사용 시 venue id
    private String performanceName, genre, castInfo, description;
    private int runningTime, maxTicketsPerMember;
    private Date showAt, bookingOpenAt, cancelDeadlineAt; // Performance와 같은 java.util.Date
    private String venueName, venueAddress;
    private List<GradeInput> grades;
    private RentalInput rental;
    private LayoutInput layout;

    public PerformanceRegisterRequest () { }
    
    public PerformanceRegisterRequest(Long venueTemplateId, String performanceName, String genre, String castInfo,
            String description, int runningTime, int maxTicketsPerMember, Date showAt,
            Date bookingOpenAt, Date cancelDeadlineAt, String venueName, String venueAddress, List<GradeInput> grades,
            RentalInput rental, LayoutInput layout) {
        super();
        this.venueTemplateId = venueTemplateId;
        this.performanceName = performanceName;
        this.genre = genre;
        this.castInfo = castInfo;
        this.description = description;
        this.runningTime = runningTime;
        this.maxTicketsPerMember = maxTicketsPerMember;
        this.showAt = showAt;
        this.bookingOpenAt = bookingOpenAt;
        this.cancelDeadlineAt = cancelDeadlineAt;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.grades = grades;
        this.rental = rental;
        this.layout = layout;
    }

    public static class GradeInput {
        String gradeKey;
        String gradeName;
        int price;
        
        public String getGradeKey() {
            return gradeKey;
        }
        
        public void setGradeKey(String gradeKey) {
            this.gradeKey = gradeKey;
        }
        
        public String getGradeName() {
            return gradeName;
        }
        
        public void setGradeName(String gradeName) {
            this.gradeName = gradeName;
        }
        
        public int getPrice() {
            return price;
        }
        
        public void setPrice(int price) {
            this.price = price;
        }
    } 

    public static class RentalInput {
        Date rentalStartAt;
        Date rentalEndAt;
        
        public Date getRentalStartAt() {
            return rentalStartAt;
        }
        
        public void setRentalStartAt(Date rentalStartAt) {
            this.rentalStartAt = rentalStartAt;
        }
        
        public Date getRentalEndAt() {
            return rentalEndAt;
        }
       
        public void setRentalEndAt(Date rentalEndAt) {
            this.rentalEndAt = rentalEndAt;
        }
    }

    public static class LayoutInput {
        String seatLayoutName;
        Long sourceLayoutId; // 템플릿에서 시작했으면 원본 layout id
        boolean shareAsTemplate;
        List<SectorInput> sectors;
        
        public boolean isShareAsTemplate() {
            return shareAsTemplate;
        }

        public void setShareAsTemplate(boolean shareAsTemplate) {
            this.shareAsTemplate = shareAsTemplate;
        }

        public String getSeatLayoutName() {
            return seatLayoutName;
        }
        
        public void setSeatLayoutName(String seatLayoutName) {
            this.seatLayoutName = seatLayoutName;
        }
        
        public Long getSourceLayoutId() {
            return sourceLayoutId;
        }
       
        public void setSourceLayoutId(Long sourceLayoutId) {
            this.sourceLayoutId = sourceLayoutId;
        }
        
        public List<SectorInput> getSectors() {
            return sectors;
        }
        
        public void setSectors(List<SectorInput> sectors) {
            this.sectors = sectors;
        }
    }

    public static class SectorInput {
        String sectorName;
        String gradeKey;
        List<SeatInput> seats;
        
        public String getSectorName() {
            return sectorName;
        }
        
        public void setSectorName(String sectorName) {
            this.sectorName = sectorName;
        }
        
        public String getGradeKey() {
            return gradeKey;
        }
        
        public void setGradeKey(String gradeKey) {
            this.gradeKey = gradeKey;
        }
        
        public List<SeatInput> getSeats() {
            return seats;
        }
        
        public void setSeats(List<SeatInput> seats) {
            this.seats = seats;
        }
    }

    public static class SeatInput {
        String rowNum;
        int columnNum;
        String seatNumber;
        
        public String getRowNum() {
            return rowNum;
        }
        
        public void setRowNum(String rowNum) {
            this.rowNum = rowNum;
        }
        
        public int getColumnNum() {
            return columnNum;
        }
        
        public void setColumnNum(int columnNum) {
            this.columnNum = columnNum;
        }
        
        public String getSeatNumber() {
            return seatNumber;
        }
        
        public void setSeatNumber(String seatNumber) {
            this.seatNumber = seatNumber;
        }
    }

    public Long getVenueTemplateId() {
        return venueTemplateId;
    }

    public void setVenueTemplateId(Long venueTemplateId) {
        this.venueTemplateId = venueTemplateId;
    }

    public String getPerformanceName() {
        return performanceName;
    }

    public void setPerformanceName(String performanceName) {
        this.performanceName = performanceName;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getCastInfo() {
        return castInfo;
    }

    public void setCastInfo(String castInfo) {
        this.castInfo = castInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRunningTime() {
        return runningTime;
    }

    public void setRunningTime(int runningTime) {
        this.runningTime = runningTime;
    }

    public int getMaxTicketsPerMember() {
        return maxTicketsPerMember;
    }

    public void setMaxTicketsPerMember(int maxTicketsPerMember) {
        this.maxTicketsPerMember = maxTicketsPerMember;
    }

    public Date getShowAt() {
        return showAt;
    }

    public void setShowAt(Date showAt) {
        this.showAt = showAt;
    }

    public Date getBookingOpenAt() {
        return bookingOpenAt;
    }

    public void setBookingOpenAt(Date bookingOpenAt) {
        this.bookingOpenAt = bookingOpenAt;
    }

    public Date getCancelDeadlineAt() {
        return cancelDeadlineAt;
    }

    public void setCancelDeadlineAt(Date cancelDeadlineAt) {
        this.cancelDeadlineAt = cancelDeadlineAt;
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

    public List<GradeInput> getGrades() {
        return grades;
    }

    public void setGrades(List<GradeInput> grades) {
        this.grades = grades;
    }

    public RentalInput getRental() {
        return rental;
    }

    public void setRental(RentalInput rental) {
        this.rental = rental;
    }

    public LayoutInput getLayout() {
        return layout;
    }

    public void setLayout(LayoutInput layout) {
        this.layout = layout;
    }
}
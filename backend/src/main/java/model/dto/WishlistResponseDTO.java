
package model.dto;

import java.sql.Timestamp;

import model.domain.Wishlist;

public class WishlistResponseDTO {

    private Wishlist wishlist;
    private String performanceTitle;
    private Timestamp performanceDate;
    private String sectorName;

    public WishlistResponseDTO() {
    }

    public WishlistResponseDTO(Wishlist wishlist, String performanceTitle,
                               Timestamp performanceDate, String sectorName) {
        this.wishlist = wishlist;
        this.performanceTitle = performanceTitle;
        this.performanceDate = performanceDate;
        this.sectorName = sectorName;
    }

    public Wishlist getWishlist() {
        return wishlist;
    }

    public void setWishlist(Wishlist wishlist) {
        this.wishlist = wishlist;
    }

    public String getPerformanceTitle() {
        return performanceTitle;
    }

    public void setPerformanceTitle(String performanceTitle) {
        this.performanceTitle = performanceTitle;
    }

    public Timestamp getPerformanceDate() {
        return performanceDate;
    }

    public void setPerformanceDate(Timestamp performanceDate) {
        this.performanceDate = performanceDate;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }
}

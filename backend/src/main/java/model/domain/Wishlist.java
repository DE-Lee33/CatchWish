
package model.domain;

import java.sql.Timestamp;

public class Wishlist {

    private Long wishlistId;
    private Long memberId;
    private Long performanceId;
    private Long sectorId;
    private Integer priority;
    private Timestamp createdAt;

    public Wishlist() {
    }

    public Wishlist(Long wishlistId, Long memberId, Long performanceId,
                    Long sectorId, Integer priority, Timestamp createdAt) {
        this.wishlistId = wishlistId;
        this.memberId = memberId;
        this.performanceId = performanceId;
        this.sectorId = sectorId;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    public Long getWishlistId() {
        return wishlistId;
    }

    public void setWishlistId(Long wishlistId) {
        this.wishlistId = wishlistId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }
    
    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

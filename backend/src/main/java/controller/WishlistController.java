package controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import common.auth.LoginMember;
import common.auth.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import model.dto.PriorityRequest;
import model.dto.WishlistRequest;
import model.dto.WishlistResponseDTO;
import model.manager.WishlistManager;
import model.dto.WishlistOrderRequest;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistManager wishlistManager;

    public WishlistController(WishlistManager wishlistManager) {
        this.wishlistManager = wishlistManager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistResponseDTO add(@RequestBody WishlistRequest body, HttpServletRequest request) throws Exception {
        LoginMember member = LoginSession.require(request);
        return wishlistManager.create(member.memberId(), body.performanceId(), body.sectorId(), body.priority());
    }

    @GetMapping
    public List<WishlistResponseDTO> list(HttpServletRequest request) throws Exception {
        LoginMember member = LoginSession.require(request);
        return wishlistManager.findWishlistList(member.memberId());
    }

    @DeleteMapping("/{wishlistId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long wishlistId, HttpServletRequest request) throws Exception {
        LoginMember member = LoginSession.require(request);
        wishlistManager.remove(wishlistId, member.memberId());
    }

    @PutMapping("/{wishlistId}/priority")
    public List<WishlistResponseDTO> updatePriority(@PathVariable Long wishlistId,
            @RequestBody PriorityRequest body, HttpServletRequest request) throws Exception {
        LoginMember member = LoginSession.require(request);
        return wishlistManager.updatePriority(wishlistId, member.memberId(), body.priority());
    }
    
    @PutMapping("/order")
    public List<WishlistResponseDTO> reorder(@RequestBody WishlistOrderRequest body, HttpServletRequest request)
            throws Exception {
        LoginMember member = LoginSession.require(request);
        return wishlistManager.reorder(member.memberId(), body.wishlistIds());
    }
}

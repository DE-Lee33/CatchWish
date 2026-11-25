package model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import model.dto.WishlistResponseDTO;

public interface WishlistMapper {
    List<WishlistResponseDTO> selectWishlistByMemberId(@Param("memberId") Long memberId);
}
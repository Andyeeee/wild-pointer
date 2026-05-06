package org.andywang.wildpointer.controller;

import org.andywang.wildpointer.common.ApiResponse;
import org.andywang.wildpointer.dto.AddFavoriteRequest;
import org.andywang.wildpointer.dto.AddFavoriteResponse;
import org.andywang.wildpointer.dto.PageResult;
import org.andywang.wildpointer.entity.Favorite;
import org.andywang.wildpointer.security.CurrentUserId;
import org.andywang.wildpointer.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @GetMapping
    public PageResult<Favorite> getUserFavorites(@CurrentUserId Integer userId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return favoriteService.getUserFavorites(userId, page, size);
    }

    @PostMapping
    public ApiResponse<AddFavoriteResponse> addFavorite(@CurrentUserId Integer userId,
                                                        @Valid @RequestBody AddFavoriteRequest request) {
        return favoriteService.addFavorite(userId, request.getName(), request.getLocation(), request.getType());
    }

    @DeleteMapping("/{favoriteId}")
    public ApiResponse<Void> deleteFavorite(@PathVariable Integer favoriteId,
                                            @CurrentUserId Integer userId) {
        return favoriteService.deleteFavorite(favoriteId, userId);
    }
}

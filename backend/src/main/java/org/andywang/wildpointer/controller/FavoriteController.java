package org.andywang.wildpointer.controller;

import org.andywang.wildpointer.entity.Favorite;
import org.andywang.wildpointer.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 获取用户的收藏
     */
    @GetMapping("/user/{userId}")
    public List<Favorite> getUserFavorites(@PathVariable Integer userId) {
        return favoriteService.getUserFavorites(userId);
    }

    /**
     * 添加收藏
     */
    @PostMapping
    public Map<String, Object> addFavorite(
            @RequestParam Integer userId,
            @RequestParam String name,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type
    ) {
        return favoriteService.addFavorite(userId, name, location, type);
    }

    /**
     * 删除收藏
     */
    @DeleteMapping("/{favoriteId}")
    public Map<String, Object> deleteFavorite(
            @PathVariable Integer favoriteId,
            @RequestParam Integer userId
    ) {
        return favoriteService.deleteFavorite(favoriteId, userId);
    }
}

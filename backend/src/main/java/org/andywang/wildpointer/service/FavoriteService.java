package org.andywang.wildpointer.service;

import org.andywang.wildpointer.entity.Favorite;
import org.andywang.wildpointer.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    /**
     * 添加收藏
     */
    public Map<String, Object> addFavorite(Integer userId, String name, String location, String type) {
        Map<String, Object> result = new HashMap<>();

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setName(name);
        favorite.setLocation(location);
        favorite.setType(type);

        favoriteRepository.save(favorite);
        result.put("success", true);
        result.put("message", "已添加到收藏");
        result.put("favoriteId", favorite.getId());
        return result;
    }

    /**
     * 获取用户的收藏
     */
    public List<Favorite> getUserFavorites(Integer userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除收藏
     */
    public Map<String, Object> deleteFavorite(Integer favoriteId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Favorite> favOpt = favoriteRepository.findById(favoriteId);
        if (!favOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "收藏不存在");
            return result;
        }

        Favorite favorite = favOpt.get();
        if (!favorite.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "无权限删除此收藏");
            return result;
        }

        favoriteRepository.deleteById(favoriteId);
        result.put("success", true);
        result.put("message", "收藏已删除");
        return result;
    }
}

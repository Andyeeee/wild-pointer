package org.andywang.wildpointer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.andywang.wildpointer.common.ApiResponse;
import org.andywang.wildpointer.dto.AddFavoriteResponse;
import org.andywang.wildpointer.dto.PageResult;
import org.andywang.wildpointer.entity.Favorite;
import org.andywang.wildpointer.mapper.FavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    public ApiResponse<AddFavoriteResponse> addFavorite(Integer userId, String name, String location, String type) {
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setName(name);
        favorite.setLocation(location);
        favorite.setType(type);
        favoriteMapper.insert(favorite);

        return ApiResponse.ok("已添加到收藏", AddFavoriteResponse.builder()
                .favoriteId(favorite.getId())
                .build());
    }

    public PageResult<Favorite> getUserFavorites(Integer userId, int page, int size) {
        Page<Favorite> pageParam = new Page<>(page, size);
        Page<Favorite> result = favoriteMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt));
        return PageResult.<Favorite>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(page)
                .size(size)
                .totalPages((int) Math.ceil((double) result.getTotal() / size))
                .build();
    }

    public ApiResponse<Void> deleteFavorite(Integer favoriteId, Integer userId) {
        Favorite favorite = favoriteMapper.selectById(favoriteId);
        if (favorite == null) {
            return ApiResponse.fail("收藏不存在");
        }
        if (!favorite.getUserId().equals(userId)) {
            return ApiResponse.fail("无权限删除此收藏");
        }

        favoriteMapper.deleteById(favoriteId);
        return ApiResponse.ok("收藏已删除");
    }
}

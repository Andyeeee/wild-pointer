package org.andywang.wildpointer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.andywang.wildpointer.common.ApiResponse;
import org.andywang.wildpointer.dto.PageResult;
import org.andywang.wildpointer.dto.SaveRouteResponse;
import org.andywang.wildpointer.entity.Route;
import org.andywang.wildpointer.mapper.RouteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    @Autowired
    private RouteMapper routeMapper;

    public ApiResponse<SaveRouteResponse> saveRoute(Integer userId, String name, String startLocation,
                                                     String endLocation, Double startLat, Double startLon,
                                                     Double endLat, Double endLon,
                                                     String distance, String duration, String routeType) {
        Route route = new Route();
        route.setUserId(userId);
        route.setName(name);
        route.setStartLocation(startLocation);
        route.setEndLocation(endLocation);
        route.setStartLat(startLat);
        route.setStartLon(startLon);
        route.setEndLat(endLat);
        route.setEndLon(endLon);
        route.setDistance(distance);
        route.setDuration(duration);
        route.setRouteType(routeType);
        routeMapper.insert(route);

        return ApiResponse.ok("路线已保存", SaveRouteResponse.builder()
                .routeId(route.getId())
                .build());
    }

    public PageResult<Route> getUserRoutes(Integer userId, int page, int size) {
        Page<Route> pageParam = new Page<>(page, size);
        Page<Route> result = routeMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Route>()
                        .eq(Route::getUserId, userId)
                        .orderByDesc(Route::getCreatedAt));
        return PageResult.<Route>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(page)
                .size(size)
                .totalPages((int) Math.ceil((double) result.getTotal() / size))
                .build();
    }

    public ApiResponse<Void> deleteRoute(Integer routeId, Integer userId) {
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return ApiResponse.fail("路线不存在");
        }
        if (!route.getUserId().equals(userId)) {
            return ApiResponse.fail("无权限删除此路线");
        }

        routeMapper.deleteById(routeId);
        return ApiResponse.ok("路线已删除");
    }
}

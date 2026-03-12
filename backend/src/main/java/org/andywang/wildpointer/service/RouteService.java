package org.andywang.wildpointer.service;

import org.andywang.wildpointer.entity.Route;
import org.andywang.wildpointer.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RouteService {

    @Autowired
    private RouteRepository routeRepository;

    /**
     * 保存新的路线记录
     */
    public Map<String, Object> saveRoute(Integer userId, String name, String startLocation, String endLocation, String distance, String duration, String routeType) {
        Map<String, Object> result = new HashMap<>();

        Route route = new Route();
        route.setUserId(userId);
        route.setName(name);
        route.setStartLocation(startLocation);
        route.setEndLocation(endLocation);
        route.setDistance(distance);
        route.setDuration(duration);
        route.setRouteType(routeType);

        routeRepository.save(route);
        result.put("success", true);
        result.put("message", "路线已保存");
        result.put("routeId", route.getId());
        return result;
    }

    /**
     * 获取用户的历史记录
     */
    public List<Route> getUserRoutes(Integer userId) {
        return routeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除路线记录
     */
    public Map<String, Object> deleteRoute(Integer routeId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Route> routeOpt = routeRepository.findById(routeId);
        if (!routeOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        Route route = routeOpt.get();
        if (!route.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "无权限删除此路线");
            return result;
        }

        routeRepository.deleteById(routeId);
        result.put("success", true);
        result.put("message", "路线已删除");
        return result;
    }
}

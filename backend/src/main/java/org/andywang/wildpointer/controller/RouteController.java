package org.andywang.wildpointer.controller;

import org.andywang.wildpointer.entity.Route;
import org.andywang.wildpointer.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    /**
     * 获取用户的历史记录
     */
    @GetMapping("/user/{userId}")
    public List<Route> getUserRoutes(@PathVariable Integer userId) {
        return routeService.getUserRoutes(userId);
    }

    /**
     * 保存新的路线记录
     */
    @PostMapping
    public Map<String, Object> saveRoute(
            @RequestParam Integer userId,
            @RequestParam String name,
            @RequestParam(required = false) String startLocation,
            @RequestParam(required = false) String endLocation,
            @RequestParam(required = false) String distance,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String routeType
    ) {
        return routeService.saveRoute(userId, name, startLocation, endLocation, distance, duration, routeType);
    }

    /**
     * 删除历史记录
     */
    @DeleteMapping("/{routeId}")
    public Map<String, Object> deleteRoute(
            @PathVariable Integer routeId,
            @RequestParam Integer userId
    ) {
        return routeService.deleteRoute(routeId, userId);
    }
}

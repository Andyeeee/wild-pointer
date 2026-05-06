package org.andywang.wildpointer.controller;

import org.andywang.wildpointer.common.ApiResponse;
import org.andywang.wildpointer.dto.PageResult;
import org.andywang.wildpointer.dto.SaveRouteRequest;
import org.andywang.wildpointer.dto.SaveRouteResponse;
import org.andywang.wildpointer.entity.Route;
import org.andywang.wildpointer.security.CurrentUserId;
import org.andywang.wildpointer.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @GetMapping
    public PageResult<Route> getUserRoutes(@CurrentUserId Integer userId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return routeService.getUserRoutes(userId, page, size);
    }

    @PostMapping
    public ApiResponse<SaveRouteResponse> saveRoute(@CurrentUserId Integer userId,
                                                     @Valid @RequestBody SaveRouteRequest request) {
        return routeService.saveRoute(userId, request.getName(), request.getStartLocation(),
                request.getEndLocation(), request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon(),
                request.getDistance(), request.getDuration(), request.getRouteType());
    }

    @DeleteMapping("/{routeId}")
    public ApiResponse<Void> deleteRoute(@PathVariable Integer routeId,
                                         @CurrentUserId Integer userId) {
        return routeService.deleteRoute(routeId, userId);
    }
}

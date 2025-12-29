package org.andywang.wildpointer.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 允许前端跨域调用
public class WildController {

    /**
     * 生成随机坐标接口
     * @param lat 当前纬度
     * @param lon 当前经度
     * @param minRadius 最小半径 (km)
     * @param maxRadius 最大半径 (km)
     */
    @GetMapping("/generate")
    public Map<String, Object> generate(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") double minRadius,
            @RequestParam(defaultValue = "50") double maxRadius) {

        // 1. 随机生成距离 (km)
        double distance = minRadius + Math.random() * (maxRadius - minRadius);

        // 2. 随机生成角度 (0 - 360 度)
        double angle = Math.random() * 360;

        // 3. 计算新坐标 (极坐标转换 - 简化版估算)
        // 地球半径平均约 6371km，这里使用简单的平移估算，对于短途旅行误差可接受
        // 纬度变化：1度 ≈ 111km
        double newLat = lat + (distance * Math.cos(Math.toRadians(angle)) / 111.0);

        // 经度变化：1度 ≈ 111km * cos(纬度)
        double newLon = lon + (distance * Math.sin(Math.toRadians(angle)) / (111.0 * Math.cos(Math.toRadians(lat))));

        // 4. 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("currentLat", lat);
        result.put("currentLon", lon);
        result.put("destLat", newLat); // 目标纬度
        result.put("destLon", newLon); // 目标经度
        result.put("distance", String.format("%.2f", distance)); // 实际距离
        result.put("angle", String.format("%.2f", angle)); // 实际角度
        result.put("msg", "Coordinates locked. Prepare for jump.");

        return result;
    }
}
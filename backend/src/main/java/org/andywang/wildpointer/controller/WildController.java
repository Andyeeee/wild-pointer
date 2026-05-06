package org.andywang.wildpointer.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.andywang.wildpointer.security.CurrentUserId;
import org.andywang.wildpointer.service.GpxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WildController {

    @Autowired
    private GpxService gpxService;

    @Value("${amap.js-api-key:}")
    private String amapJsApiKey;

    @Value("${amap.security-code:}")
    private String amapSecurityCode;

    @Value("${amap.web-service-key:}")
    private String amapWebKey;

    @GetMapping("/config/amap")
    public Map<String, String> getAmapConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("key", amapJsApiKey);
        config.put("securityCode", amapSecurityCode);
        return config;
    }

    @GetMapping("/generate-random")
    public Map<String, Object> generateRandom(
            @CurrentUserId Integer userId,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double minRadius,
            @RequestParam double maxRadius,
            @RequestParam(defaultValue = "false") boolean useGpx
    ) {
        double[] randomPoint = calculateRandomPoint(lat, lon, minRadius, maxRadius);
        double[] snappedPoint = roadSnapping(randomPoint[0], randomPoint[1]);
        boolean isUnvisited = !checkIsVisited(userId, snappedPoint[0], snappedPoint[1]);

        Map<String, Object> result = new HashMap<>();
        result.put("destLat", snappedPoint[0]);
        result.put("destLon", snappedPoint[1]);
        result.put("isFogCleared", isUnvisited);
        return result;
    }

    @GetMapping("/generate-waypoint")
    public Map<String, Object> generateWaypoint(
            @CurrentUserId Integer userId,
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon,
            @RequestParam(defaultValue = "true") boolean useGpx
    ) {
        double[] finalPoint = new double[2];
        boolean found = false;
        int maxRetries = 20;
        int attempt = 0;

        do {
            attempt++;
            double progress = 0.2 + (Math.random() * 0.6);
            double baseLat = startLat + (endLat - startLat) * progress;
            double baseLon = startLon + (endLon - startLon) * progress;

            double deviationRadius = 1.0 + (Math.random() * 2.0);
            double[] candidate = calculateRandomPoint(baseLat, baseLon, 0, deviationRadius);

            if (useGpx && checkIsVisited(userId, candidate[0], candidate[1])) {
                continue;
            }

            finalPoint = roadSnapping(candidate[0], candidate[1]);
            found = true;
            break;

        } while (attempt < maxRetries);

        if (!found) {
            finalPoint = roadSnapping(startLat, startLon);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("wayLat", finalPoint[0]);
        result.put("wayLon", finalPoint[1]);
        return result;
    }

    private boolean checkIsVisited(Integer userId, double lat, double lon) {
        if (userId == null) return false;
        return gpxService.isVisited(userId, lat, lon);
    }

    private double[] calculateRandomPoint(double lat, double lon, double minKm, double maxKm) {
        double angle = Math.random() * 360;
        double distance = minKm + (maxKm - minKm) * Math.random();
        double dLat = distance * Math.cos(Math.toRadians(angle)) / 111.0;
        double dLon = distance * Math.sin(Math.toRadians(angle)) / (111.0 * Math.cos(Math.toRadians(lat)));
        return new double[]{lat + dLat, lon + dLon};
    }

    private double[] roadSnapping(double lat, double lon) {
        try {
            String url = String.format(
                    "https://restapi.amap.com/v3/geocode/regeo?key=%s&location=%f,%f&extensions=all&radius=1000&roadlevel=0",
                    amapWebKey, lon, lat);
            String response = HttpUtil.get(url);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONObject regeocode = json.getJSONObject("regeocode");
                JSONArray roads = regeocode.getJSONArray("roads");

                if (roads != null && !roads.isEmpty()) {
                    JSONObject nearestRoad = roads.getJSONObject(0);
                    String locationStr = nearestRoad.getStr("location");
                    String[] split = locationStr.split(",");
                    return new double[]{
                            Double.parseDouble(split[1]),
                            Double.parseDouble(split[0])
                    };
                }
            }
        } catch (Exception e) {
            System.err.println("道路吸附失败，降级使用原始坐标: " + e.getMessage());
        }
        return new double[]{lat, lon};
    }
}

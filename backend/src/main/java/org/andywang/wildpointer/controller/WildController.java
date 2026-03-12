package org.andywang.wildpointer.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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

    // 从环境变量读取高德API配置（更安全）
    @Value("${amap.js-api-key:}")
    private String amapJsApiKey;

    @Value("${amap.security-code:}")
    private String amapSecurityCode;

    // 替换为你的高德 Web服务 Key (注意不是 JS API 的 Key)
    // 这里的 Key 必须支持 "Web服务" 类型的 API
    @Value("${amap.web-service-key:}")
    private String amapWebKey;

    /**
     * 新增：获取高德地图前端配置接口
     * 前端需要的配置由后端统一提供，避免在前端暴露API密钥
     */
    @GetMapping("/config/amap")
    public Map<String, String> getAmapConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("key", amapJsApiKey);
        config.put("securityCode", amapSecurityCode);
        return config;
    }

    /**
     * 模式一：完全随机生成接口
     * @param lat 当前纬度
     * @param lon 当前经度
     * @param minRadius 最小半径 (km)
     * @param maxRadius 最大半径 (km)
     * @param useGpx 是否使用迷雾过滤 (Phase 3 实现，目前先预留)
     */
    @GetMapping("/generate-random")
    public Map<String, Object> generateRandom(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double minRadius,
            @RequestParam double maxRadius,
            @RequestParam(defaultValue = "false") boolean useGpx
    ) {
        // 1. 数学生成：先算出一个"野点" (可能在水里)
        double[] randomPoint = calculateRandomPoint(lat, lon, minRadius, maxRadius);

        // 2. 道路吸附：把"野点"纠正到最近的路上
        // 如果吸附失败（比如深山老林没有路），就降级返回原始点
        double[] snappedPoint = roadSnapping(randomPoint[0], randomPoint[1]);

        // 3. 构造返回
        Map<String, Object> result = new HashMap<>();
        result.put("destLat", snappedPoint[0]);
        result.put("destLon", snappedPoint[1]);
        // Phase 3 预留：这里未来会去查数据库判重
        result.put("isFogCleared", false);

        return result;
    }

    /**
     * 模式二：中间途经点生成接口 (最终逻辑版)
     * 核心逻辑：沿途探索 + 迷雾筛选 (Rejection Sampling)
     */
    @GetMapping("/generate-waypoint")
    public Map<String, Object> generateWaypoint(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon,
            @RequestParam(defaultValue = "true") boolean useGpx
    ) {
        double[] finalPoint = new double[2];
        boolean found = false;

        // 最大尝试次数 (防止死循环)
        // 如果试了 20 次发现周围全是去过的路，就放弃筛选，随便给一个
        int maxRetries = 20;
        int attempt = 0;

        do {
            attempt++;

            // 1. 【沿途生成】：在起终点的大方向上找基准点
            // progress: 随机取路径的 20% ~ 80% 处
            double progress = 0.2 + (Math.random() * 0.6);
            double baseLat = startLat + (endLat - startLat) * progress;
            double baseLon = startLon + (endLon - startLon) * progress;

            // 2. 【随机扰动】：在基准点旁边 1km ~ 3km 找一个偏移点
            double deviationRadius = 1.0 + (Math.random() * 2.0);
            double[] candidate = calculateRandomPoint(baseLat, baseLon, 0, deviationRadius);

            // 3. 【迷雾筛选】：核心逻辑！
            // 如果开启了迷雾模式，并且这个点已经去过了，就 continue (重试)
            if (useGpx && checkIsVisited(candidate[0], candidate[1])) {
                // System.out.println("第" + attempt + "次尝试：该点已探索，重试...");
                continue;
            }

            // 4. 【道路吸附】：只有通过了迷雾筛选的点，才值得去吸附
            // 这样能节省高德 API 的调用次数
            finalPoint = roadSnapping(candidate[0], candidate[1]);

            // 成功找到
            found = true;
            break;

        } while (attempt < maxRetries);

        // 如果循环20次都没找到(说明这片区域你太熟了)，就兜底返回最后一次计算的点
        if (!found) {
            finalPoint = roadSnapping(startLat, startLon); // 极端情况兜底
        }

        Map<String, Object> result = new HashMap<>();
        result.put("wayLat", finalPoint[0]);
        result.put("wayLon", finalPoint[1]);

        return result;
    }

    /**
     * 🔥 Phase 3 预留接口：检查某个坐标是否在历史轨迹中
     * 目前 Phase 1：永远返回 false (代表没去过)
     * 未来 Phase 3：这里会执行 SQL 查询：SELECT count(*) FROM tracks WHERE ST_Distance(...) < 500
     */
    private boolean checkIsVisited(double lat, double lon) {
        // TODO: 等有了数据库，在这里填入真正的查询逻辑
        // 伪代码: return trackService.isPointVisited(lat, lon);

        return false; // 暂时假设所有地方都没去过
    }

    // ================== 下面是核心工具方法 ==================

    /**
     * 核心算法：生成环形随机点
     * 原理：极坐标变换
     */
    private double[] calculateRandomPoint(double lat, double lon, double minKm, double maxKm) {
        // 地球半径常量
        double EARTH_RADIUS = 6371.0;

        // 随机角度 (0 ~ 360)
        double angle = Math.random() * 360;
        // 随机距离 (min ~ max)
        double distance = minKm + (maxKm - minKm) * Math.random();

        // 极坐标转换 (简化版，短距离内误差可忽略)
        double dLat = distance * Math.cos(Math.toRadians(angle)) / 111.0;
        double dLon = distance * Math.sin(Math.toRadians(angle)) / (111.0 * Math.cos(Math.toRadians(lat)));

        return new double[]{lat + dLat, lon + dLon};
    }

    /**
     * 核心算法：道路吸附 (调用高德逆地理编码 API)
     * 文档：https://lbs.amap.com/api/webservice/guide/api/georegeo
     */
    private double[] roadSnapping(double lat, double lon) {
        try {
            // 构造高德 API 请求
            // extensions=all 会返回 pois 和 roads
            // radius=1000 查找周边 1km 内的道路
            String url = String.format(
                    "https://restapi.amap.com/v3/geocode/regeo?key=%s&location=%f,%f&extensions=all&radius=1000&roadlevel=0",
                    amapWebKey, lon, lat // 高德接口是 经度,纬度
            );

            // 发送 HTTP GET 请求
            String response = HttpUtil.get(url);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONObject regeocode = json.getJSONObject("regeocode");
                // 获取道路列表
                JSONArray roads = regeocode.getJSONArray("roads");

                if (roads != null && !roads.isEmpty()) {
                    // 取第一条路（最近的路）
                    JSONObject nearestRoad = roads.getJSONObject(0);
                    // 拿到这条路的一个坐标点 (location string: "lon,lat")
                    String locationStr = nearestRoad.getStr("location");
                    String[] split = locationStr.split(",");

                    // 只有当 API 返回有效数据时，才使用吸附后的坐标
                    // 注意：API 返回的是 lon, lat
                    return new double[]{
                            Double.parseDouble(split[1]),
                            Double.parseDouble(split[0])
                    };
                }
            }
        } catch (Exception e) {
            System.err.println("道路吸附失败，降级使用原始坐标: " + e.getMessage());
        }

        // 如果吸附失败，原样返回
        return new double[]{lat, lon};
    }

    // 辅助：计算两点距离 (km)
    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

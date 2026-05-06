package org.andywang.wildpointer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.andywang.wildpointer.dto.FogCellResponse;
import org.andywang.wildpointer.dto.GpxUploadResponse;
import org.andywang.wildpointer.entity.ExploredCell;
import org.andywang.wildpointer.mapper.ExploredCellMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GpxService {

    @Autowired
    private ExploredCellMapper exploredCellMapper;

    private static final double GRID_SCALE = 10000.0;
    private static final Pattern WPT_PATTERN = Pattern.compile(
            "<wpt\\s+lat=\"([^\"]+)\"\\s+lon=\"([^\"]+)\"");

    // Per-user cache: userId -> set of grid keys
    private final ConcurrentHashMap<Integer, Set<Long>> userCache = new ConcurrentHashMap<>();

    /**
     * Check if a point is in an explored area for a specific user
     */
    public boolean isVisited(Integer userId, double lat, double lon) {
        if (userId == null) return false;
        Set<Long> cells = userCache.computeIfAbsent(userId, this::loadUserCellsFromDb);
        if (cells.isEmpty()) return false;

        int latGrid = (int) Math.round(lat * GRID_SCALE);
        int lonGrid = (int) Math.round(lon * GRID_SCALE);

        // Check target cell and 8 neighbors
        for (int dLat = -1; dLat <= 1; dLat++) {
            for (int dLon = -1; dLon <= 1; dLon++) {
                long key = ((long) (latGrid + dLat) * 10_000_000L) + (lonGrid + dLon);
                if (cells.contains(key)) return true;
            }
        }
        return false;
    }

    /**
     * Parse GPX file and store explored cells for a user
     */
    public GpxUploadResponse parseAndStoreGpx(MultipartFile file, Integer userId) throws IOException {
        // 1. Parse GPX
        Set<Long> uniqueKeys = new HashSet<>();
        int totalPoints = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = WPT_PATTERN.matcher(line);
                if (matcher.find()) {
                    double lat = Double.parseDouble(matcher.group(1));
                    double lon = Double.parseDouble(matcher.group(2));
                    // GPX 使用 WGS-84，高德地图使用 GCJ-02，需要转换
                    double[] gcj = wgs84ToGcj02(lat, lon);
                    uniqueKeys.add(gridKey(gcj[0], gcj[1]));
                    totalPoints++;
                }
            }
        }

        // 2. Delete existing cells for this user (replace strategy)
        exploredCellMapper.delete(
                new LambdaQueryWrapper<ExploredCell>().eq(ExploredCell::getUserId, userId));

        // 3. Batch insert, 1000 records per batch
        List<ExploredCell> batch = new ArrayList<>(1000);
        int inserted = 0;
        for (Long key : uniqueKeys) {
            int gridLat = (int) (key / 10_000_000L);
            int gridLon = (int) (key % 10_000_000L);
            ExploredCell cell = new ExploredCell();
            cell.setUserId(userId);
            cell.setGridLat(gridLat);
            cell.setGridLon(gridLon);
            batch.add(cell);
            if (batch.size() >= 1000) {
                inserted += exploredCellMapper.batchInsertIgnore(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            inserted += exploredCellMapper.batchInsertIgnore(batch);
        }

        // 4. Invalidate cache
        invalidateUserCache(userId);

        return GpxUploadResponse.builder()
                .totalPoints(totalPoints)
                .uniqueCells(uniqueKeys.size())
                .newCellsAdded(inserted)
                .build();
    }

    /**
     * Get explored cells within a viewport (for frontend fog rendering)
     */
    public List<FogCellResponse> getCellsInViewport(Integer userId, double southLat, double northLat,
                                                     double westLon, double eastLon) {
        int gridSouth = (int) Math.floor(southLat * GRID_SCALE);
        int gridNorth = (int) Math.ceil(northLat * GRID_SCALE);
        int gridWest = (int) Math.floor(westLon * GRID_SCALE);
        int gridEast = (int) Math.ceil(eastLon * GRID_SCALE);

        List<ExploredCell> cells = exploredCellMapper.selectList(
                new LambdaQueryWrapper<ExploredCell>()
                        .eq(ExploredCell::getUserId, userId)
                        .ge(ExploredCell::getGridLat, gridSouth)
                        .le(ExploredCell::getGridLat, gridNorth)
                        .ge(ExploredCell::getGridLon, gridWest)
                        .le(ExploredCell::getGridLon, gridEast));

        List<FogCellResponse> result = new ArrayList<>(cells.size());
        for (ExploredCell cell : cells) {
            result.add(new FogCellResponse(cell.getGridLat(), cell.getGridLon()));
        }
        return result;
    }

    /**
     * Get all explored cells for a user (for full fog rendering)
     */
    public List<FogCellResponse> getAllCells(Integer userId) {
        List<ExploredCell> cells = exploredCellMapper.selectList(
                new LambdaQueryWrapper<ExploredCell>().eq(ExploredCell::getUserId, userId));
        List<FogCellResponse> result = new ArrayList<>(cells.size());
        for (ExploredCell cell : cells) {
            result.add(new FogCellResponse(cell.getGridLat(), cell.getGridLon()));
        }
        return result;
    }

    /**
     * Get total explored cell count for a user
     */
    public long getUserCellCount(Integer userId) {
        return exploredCellMapper.selectCount(
                new LambdaQueryWrapper<ExploredCell>().eq(ExploredCell::getUserId, userId));
    }

    /**
     * Invalidate cached data for a user
     */
    public void invalidateUserCache(Integer userId) {
        userCache.remove(userId);
    }

    private Set<Long> loadUserCellsFromDb(Integer userId) {
        List<ExploredCell> cells = exploredCellMapper.selectList(
                new LambdaQueryWrapper<ExploredCell>().eq(ExploredCell::getUserId, userId));
        Set<Long> keys = new HashSet<>(cells.size());
        for (ExploredCell cell : cells) {
            keys.add(((long) cell.getGridLat() * 10_000_000L) + cell.getGridLon());
        }
        return keys;
    }

    private long gridKey(double lat, double lon) {
        int latGrid = (int) Math.round(lat * GRID_SCALE);
        int lonGrid = (int) Math.round(lon * GRID_SCALE);
        // 使用更大的基数避免溢出（经度最大约180度 = 1800000）
        return ((long) latGrid * 10_000_000L) + lonGrid;
    }

    // WGS-84 → GCJ-02 坐标转换（国测局偏移）
    private static final double PI = 3.14159265358979324;
    private static final double A = 6378245.0; // 克拉索夫斯基椭球长半轴
    private static final double EE = 0.00669342162296594; // 偏心率平方

    private double[] wgs84ToGcj02(double lat, double lon) {
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{lat + dLat, lon + dLon};
    }

    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
}

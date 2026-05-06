package org.andywang.wildpointer.controller;

import org.andywang.wildpointer.common.ApiResponse;
import org.andywang.wildpointer.dto.FogCellResponse;
import org.andywang.wildpointer.dto.FogStatsResponse;
import org.andywang.wildpointer.dto.GpxUploadResponse;
import org.andywang.wildpointer.security.CurrentUserId;
import org.andywang.wildpointer.service.GpxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/fog")
public class FogController {

    @Autowired
    private GpxService gpxService;

    @PostMapping("/upload")
    public ApiResponse<GpxUploadResponse> uploadGpx(
            @CurrentUserId Integer userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ApiResponse.fail("请选择GPX文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".gpx")) {
            return ApiResponse.fail("只能上传.gpx文件");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            return ApiResponse.fail("文件大小不能超过20MB");
        }

        GpxUploadResponse result = gpxService.parseAndStoreGpx(file, userId);
        return ApiResponse.ok("GPX上传成功", result);
    }

    @GetMapping("/cells")
    public ApiResponse<List<FogCellResponse>> getCellsInViewport(
            @CurrentUserId Integer userId,
            @RequestParam double southLat,
            @RequestParam double northLat,
            @RequestParam double westLon,
            @RequestParam double eastLon) {
        List<FogCellResponse> cells = gpxService.getCellsInViewport(
                userId, southLat, northLat, westLon, eastLon);
        return ApiResponse.ok(cells);
    }

    @GetMapping("/all-cells")
    public ApiResponse<List<FogCellResponse>> getAllCells(@CurrentUserId Integer userId) {
        return ApiResponse.ok(gpxService.getAllCells(userId));
    }

    @GetMapping("/stats")
    public ApiResponse<FogStatsResponse> getFogStats(@CurrentUserId Integer userId) {
        long count = gpxService.getUserCellCount(userId);
        return ApiResponse.ok(FogStatsResponse.builder().totalCells(count).build());
    }
}

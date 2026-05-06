package org.andywang.wildpointer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SaveRouteRequest {
    @NotBlank(message = "路线名称不能为空")
    @Size(max = 200, message = "路线名称不能超过200个字符")
    private String name;

    @Size(max = 200, message = "起点位置不能超过200个字符")
    private String startLocation;

    @Size(max = 200, message = "终点位置不能超过200个字符")
    private String endLocation;

    private Double startLat;
    private Double startLon;
    private Double endLat;
    private Double endLon;

    @Size(max = 50, message = "距离格式不正确")
    private String distance;

    @Size(max = 50, message = "时长格式不正确")
    private String duration;

    private String routeType;
}

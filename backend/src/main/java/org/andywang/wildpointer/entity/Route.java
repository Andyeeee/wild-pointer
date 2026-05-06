package org.andywang.wildpointer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("routes")
@Data
@NoArgsConstructor
public class Route {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    private String name;

    @TableField("start_location")
    private String startLocation;

    @TableField("end_location")
    private String endLocation;

    @TableField("start_lat")
    private Double startLat;

    @TableField("start_lon")
    private Double startLon;

    @TableField("end_lat")
    private Double endLat;

    @TableField("end_lon")
    private Double endLon;

    private String distance;

    private String duration;

    @TableField("route_type")
    private String routeType;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

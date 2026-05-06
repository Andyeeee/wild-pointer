package org.andywang.wildpointer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("explored_cells")
@Data
@NoArgsConstructor
public class ExploredCell {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("grid_lat")
    private Integer gridLat;

    @TableField("grid_lon")
    private Integer gridLon;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

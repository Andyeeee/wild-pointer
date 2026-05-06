package org.andywang.wildpointer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("users")
@Data
@NoArgsConstructor
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    private String password;

    private String email;

    private String nickname;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_active")
    private Boolean isActive = true;

    private String avatar;

    private String bio;

    @TableField("default_distance")
    private Integer defaultDistance;

    @TableField("default_duration")
    private Integer defaultDuration;
}

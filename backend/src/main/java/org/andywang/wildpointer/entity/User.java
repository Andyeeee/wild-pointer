package org.andywang.wildpointer.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password; // 加密后的密码

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 50)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(length = 500)
    private String avatar; // 用户头像URL

    @Column(length = 500)
    private String bio; // 个人简介

    @Column(name = "default_distance")
    private Integer defaultDistance; // 默认探索距离（单位：米）

    @Column(name = "default_duration")
    private Integer defaultDuration; // 默认探索时长（单位：分钟）

    // 预设钩子：自动设置创建时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // 预设钩子：自动更新修改时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

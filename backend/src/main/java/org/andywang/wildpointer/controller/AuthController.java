package org.andywang.wildpointer.controller;

import org.andywang.wildpointer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false) String nickname
    ) {
        return userService.register(username, password, email, nickname);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestParam String username,
            @RequestParam String password
    ) {
        return userService.login(username, password);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserInfo(@PathVariable Long userId) {
        return userService.getUserInfo(userId);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        result.put("message", "Auth service is running");
        return result;
    }

    /**
     * 更新用户资料
     */
    @PatchMapping("/profile/{userId}")
    public Map<String, Object> updateProfile(
            @PathVariable Integer userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String bio
    ) {
        return userService.updateProfile(userId, nickname, email, bio);
    }

    /**
     * 更新偏好设置
     */
    @PatchMapping("/preferences/{userId}")
    public Map<String, Object> updatePreferences(
            @PathVariable Integer userId,
            @RequestParam(required = false) Integer defaultDistance,
            @RequestParam(required = false) Integer defaultDuration
    ) {
        return userService.updatePreferences(userId, defaultDistance, defaultDuration);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @RequestParam Integer userId,
            @RequestParam String currentPassword,
            @RequestParam String newPassword
    ) {
        return userService.changePassword(userId, currentPassword, newPassword);
    }
}

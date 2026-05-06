package org.andywang.wildpointer.controller;

import org.andywang.wildpointer.common.ApiResponse;
import org.andywang.wildpointer.dto.*;
import org.andywang.wildpointer.security.CurrentUserId;
import org.andywang.wildpointer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request.getUsername(), request.getPassword(),
                request.getEmail(), request.getNickname());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword());
    }

    @GetMapping("/user/me")
    public ApiResponse<UserInfoResponse> getUserInfo(@CurrentUserId Integer userId) {
        return userService.getUserInfo(userId);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        result.put("message", "Auth service is running");
        return result;
    }

    @PatchMapping("/profile")
    public ApiResponse<Void> updateProfile(@CurrentUserId Integer userId,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request.getNickname(), request.getEmail(), request.getBio());
    }

    @PatchMapping("/preferences")
    public ApiResponse<Void> updatePreferences(@CurrentUserId Integer userId,
                                               @Valid @RequestBody UpdatePreferencesRequest request) {
        return userService.updatePreferences(userId, request.getDefaultDistance(), request.getDefaultDuration());
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@CurrentUserId Integer userId,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
    }

    @GetMapping("/stats")
    public ApiResponse<UserStatsResponse> getUserStats(@CurrentUserId Integer userId) {
        return userService.getUserStats(userId);
    }

    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(@CurrentUserId Integer userId,
                                            @RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(userId, file);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return userService.sendVerificationCode(request.getEmail());
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
    }
}

package org.andywang.wildpointer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.andywang.wildpointer.common.ApiResponse;
import org.andywang.wildpointer.dto.*;
import org.andywang.wildpointer.entity.Route;
import org.andywang.wildpointer.entity.User;
import org.andywang.wildpointer.mapper.FavoriteMapper;
import org.andywang.wildpointer.mapper.RouteMapper;
import org.andywang.wildpointer.mapper.UserMapper;
import org.andywang.wildpointer.security.JwtUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RouteMapper routeMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";

    public ApiResponse<RegisterResponse> register(String username, String password, String email, String nickname) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            return ApiResponse.fail("用户名已存在");
        }

        count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (count > 0) {
            return ApiResponse.fail("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setEmail(email);
        user.setNickname(nickname != null ? nickname : username);
        user.setIsActive(true);
        userMapper.insert(user);

        return ApiResponse.ok("注册成功", RegisterResponse.builder()
                .userId(user.getId())
                .token(jwtUtil.generateToken(user.getId(), user.getUsername()))
                .build());
    }

    public ApiResponse<LoginResponse> login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if (!user.getIsActive()) {
            return ApiResponse.fail("账户已被禁用");
        }
        if (!verifyPassword(password, user.getPassword())) {
            return ApiResponse.fail("用户名或密码错误");
        }

        // 旧 MD5 密码自动升级为 BCrypt
        if (isLegacyMd5Hash(user.getPassword())) {
            user.setPassword(encryptPassword(password));
            userMapper.updateById(user);
        }

        return ApiResponse.ok("登录成功", LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getId(), user.getUsername()))
                .userId(user.getId())
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .defaultDistance(user.getDefaultDistance())
                .defaultDuration(user.getDefaultDuration())
                .createdAt(user.getCreatedAt())
                .build());
    }

    public ApiResponse<UserInfoResponse> getUserInfo(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        return ApiResponse.ok(UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .defaultDistance(user.getDefaultDistance())
                .defaultDuration(user.getDefaultDuration())
                .createdAt(user.getCreatedAt())
                .build());
    }

    public ApiResponse<Void> updateProfile(Integer userId, String nickname, String email, String bio) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        if (email != null && !email.equals(user.getEmail())) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (count > 0) {
                return ApiResponse.fail("邮箱已被使用");
            }
        }

        if (nickname != null && !nickname.trim().isEmpty()) user.setNickname(nickname);
        if (email != null && !email.trim().isEmpty()) user.setEmail(email);
        if (bio != null) user.setBio(bio);

        userMapper.updateById(user);
        return ApiResponse.ok("资料更新成功");
    }

    public ApiResponse<Void> updatePreferences(Integer userId, Integer defaultDistance, Integer defaultDuration) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        if (defaultDistance != null && defaultDistance > 0) user.setDefaultDistance(defaultDistance);
        if (defaultDuration != null && defaultDuration > 0) user.setDefaultDuration(defaultDuration);

        userMapper.updateById(user);
        return ApiResponse.ok("偏好设置更新成功");
    }

    public ApiResponse<Void> changePassword(Integer userId, String currentPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if (!verifyPassword(currentPassword, user.getPassword())) {
            return ApiResponse.fail("当前密码错误");
        }

        user.setPassword(encryptPassword(newPassword));
        userMapper.updateById(user);
        return ApiResponse.ok("密码修改成功");
    }

    public ApiResponse<UserStatsResponse> getUserStats(Integer userId) {
        Long routeCount = routeMapper.selectCount(
                new LambdaQueryWrapper<Route>().eq(Route::getUserId, userId));
        Long favoriteCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<org.andywang.wildpointer.entity.Favorite>()
                        .eq(org.andywang.wildpointer.entity.Favorite::getUserId, userId));

        // Calculate total distance
        java.util.List<Route> routes = routeMapper.selectList(
                new LambdaQueryWrapper<Route>().eq(Route::getUserId, userId));
        double totalKm = 0;
        for (Route route : routes) {
            if (route.getDistance() != null) {
                try {
                    String dist = route.getDistance().replaceAll("[^0-9.]", "");
                    if (!dist.isEmpty()) {
                        totalKm += Double.parseDouble(dist);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return ApiResponse.ok(UserStatsResponse.builder()
                .totalRoutes(routeCount)
                .totalFavorites(favoriteCount)
                .totalDistance(String.format("%.1f km", totalKm))
                .build());
    }

    public ApiResponse<String> uploadAvatar(Integer userId, MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail("请选择文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.fail("只能上传图片文件");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.fail("图片大小不能超过5MB");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String fileName = UUID.randomUUID().toString() + ext;

        File uploadDir = new File(AVATAR_UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        try {
            file.transferTo(new File(uploadDir, fileName));
        } catch (IOException e) {
            return ApiResponse.fail("文件上传失败");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        String avatarUrl = "/uploads/avatars/" + fileName;
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);

        return ApiResponse.ok("头像上传成功", avatarUrl);
    }

    public ApiResponse<Void> resetPassword(String email, String code, String newPassword) {
        String storedCode = verificationCodes.get(email);
        if (storedCode == null || !storedCode.equals(code)) {
            return ApiResponse.fail("验证码错误或已过期");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            return ApiResponse.fail("该邮箱未注册");
        }

        user.setPassword(encryptPassword(newPassword));
        userMapper.updateById(user);
        verificationCodes.remove(email);
        return ApiResponse.ok("密码重置成功");
    }

    // Simple in-memory verification code store
    private static final java.util.Map<String, String> verificationCodes = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Long> codeExpiry = new java.util.concurrent.ConcurrentHashMap<>();

    public ApiResponse<Void> sendVerificationCode(String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            return ApiResponse.fail("该邮箱未注册");
        }

        // Check rate limit (60 seconds)
        Long lastSent = codeExpiry.get(email + "_last");
        if (lastSent != null && System.currentTimeMillis() - lastSent < 60000) {
            return ApiResponse.fail("请60秒后再试");
        }

        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        verificationCodes.put(email, code);
        codeExpiry.put(email + "_last", System.currentTimeMillis());
        // Code expires in 10 minutes
        codeExpiry.put(email, System.currentTimeMillis() + 600000);

        // Log the code for development (in production, send via email service)
        System.out.println("=== 验证码 === Email: " + email + " Code: " + code + " ===");

        return ApiResponse.ok("验证码已发送到邮箱");
    }

    private String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private boolean verifyPassword(String rawPassword, String storedHash) {
        if (isLegacyMd5Hash(storedHash)) {
            return DigestUtils.md5Hex(rawPassword + "wildpointer_salt").equals(storedHash);
        }
        return passwordEncoder.matches(rawPassword, storedHash);
    }

    private boolean isLegacyMd5Hash(String hash) {
        return hash != null && hash.matches("[a-f0-9]{32}");
    }
}

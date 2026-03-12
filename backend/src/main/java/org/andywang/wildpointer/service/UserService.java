package org.andywang.wildpointer.service;

import org.andywang.wildpointer.entity.User;
import org.andywang.wildpointer.repository.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户注册
     */
    public Map<String, Object> register(String username, String password, String email, String nickname) {
        Map<String, Object> result = new HashMap<>();

        // 验证
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return result;
        }

        if (password == null || password.length() < 6) {
            result.put("success", false);
            result.put("message", "密码长度至少6位");
            return result;
        }

        if (email == null || !email.contains("@")) {
            result.put("success", false);
            result.put("message", "邮箱格式不正确");
            return result;
        }

        // 检查用户名是否存在
        if (userRepository.existsByUsername(username)) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        // 检查邮箱是否存在
        if (userRepository.existsByEmail(email)) {
            result.put("success", false);
            result.put("message", "邮箱已被注册");
            return result;
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setEmail(email);
        user.setNickname(nickname != null ? nickname : username);
        user.setIsActive(true);

        userRepository.save(user);

        result.put("success", true);
        result.put("message", "注册成功");
        result.put("userId", user.getId());

        return result;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        if (username == null || username.trim().isEmpty() || password == null) {
            result.put("success", false);
            result.put("message", "用户名或密码为空");
            return result;
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();

        // 检查账户是否激活
        if (!user.getIsActive()) {
            result.put("success", false);
            result.put("message", "账户已被禁用");
            return result;
        }

        // 验证密码
        if (!verifyPassword(password, user.getPassword())) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }

        // 登录成功
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("userId", user.getId());
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("avatar", user.getAvatar());
        result.put("bio", user.getBio());
        result.put("defaultDistance", user.getDefaultDistance());
        result.put("defaultDuration", user.getDefaultDuration());
        result.put("createdAt", user.getCreatedAt());

        return result;
    }

    /**
     * 加密密码 (MD5)
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5Hex(password + "wildpointer_salt");
    }

    /**
     * 验证密码
     */
    private boolean verifyPassword(String rawPassword, String encryptedPassword) {
        return encryptPassword(rawPassword).equals(encryptedPassword);
    }

    /**
     * 获取用户信息
     */
    public Map<String, Object> getUserInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userId);

        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();
        result.put("success", true);
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("avatar", user.getAvatar());
        result.put("bio", user.getBio());
        result.put("defaultDistance", user.getDefaultDistance());
        result.put("defaultDuration", user.getDefaultDuration());
        result.put("createdAt", user.getCreatedAt());

        return result;
    }

    /**
     * 更新用户资料
     */
    public Map<String, Object> updateProfile(Integer userId, String nickname, String email, String bio) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById((long) userId);
        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();

        // 如果修改邮箱，需要检查是否已存在
        if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            result.put("success", false);
            result.put("message", "邮箱已被使用");
            return result;
        }

        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname);
        }
        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email);
        }
        if (bio != null) {
            user.setBio(bio);
        }

        userRepository.save(user);
        result.put("success", true);
        result.put("message", "资料更新成功");
        return result;
    }

    /**
     * 更新偏好设置
     */
    public Map<String, Object> updatePreferences(Integer userId, Integer defaultDistance, Integer defaultDuration) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById((long) userId);
        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();
        if (defaultDistance != null && defaultDistance > 0) {
            user.setDefaultDistance(defaultDistance);
        }
        if (defaultDuration != null && defaultDuration > 0) {
            user.setDefaultDuration(defaultDuration);
        }

        userRepository.save(user);
        result.put("success", true);
        result.put("message", "偏好设置更新成功");
        return result;
    }

    /**
     * 修改密码
     */
    public Map<String, Object> changePassword(Integer userId, String currentPassword, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        Optional<User> userOpt = userRepository.findById((long) userId);
        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();

        // 验证当前密码
        if (!verifyPassword(currentPassword, user.getPassword())) {
            result.put("success", false);
            result.put("message", "当前密码错误");
            return result;
        }

        // 新密码验证
        if (newPassword == null || newPassword.length() < 6) {
            result.put("success", false);
            result.put("message", "新密码长度至少6位");
            return result;
        }

        user.setPassword(encryptPassword(newPassword));
        userRepository.save(user);
        result.put("success", true);
        result.put("message", "密码修改成功");
        return result;
    }
}

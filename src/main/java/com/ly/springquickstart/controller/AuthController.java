// src/main/java/com/ly/springquickstart/controller/AuthController.java
package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");

        String sql = "SELECT id, username, password, role, status FROM users WHERE username = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username);

        if (users.isEmpty()) {
            return Result.error("用户名或密码错误");
        }

        Map<String, Object> dbUser = users.get(0);
        String storedPassword = (String) dbUser.get("password");

        // BCrypt 校验；兼容旧明文密码（匹配后自动升级为 BCrypt）
        boolean matched;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            matched = passwordEncoder.matches(password, storedPassword);
        } else {
            matched = password.equals(storedPassword);
            if (matched) {
                jdbcTemplate.update("UPDATE users SET password = ? WHERE id = ?",
                        passwordEncoder.encode(password), dbUser.get("id"));
            }
        }
        if (!matched) {
            return Result.error("用户名或密码错误");
        }

        Integer status = (Integer) dbUser.get("status");
        if (status != null && status == 0) {
            return Result.error("账号已被封禁，请联系客服");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id",       dbUser.get("id"));
        claims.put("username", dbUser.get("username"));
        claims.put("role",     dbUser.get("role"));
        String token = JwtUtils.genToken(claims);

        // 查询完整用户信息（含 nickname、avatar、phone）
        Map<String, Object> fullUser;
        try {
            fullUser = jdbcTemplate.queryForMap(
                    "SELECT id, username, role, nickname, avatar, phone, created_at FROM users WHERE id = ?",
                    dbUser.get("id"));
        } catch (Exception e) {
            fullUser = dbUser;
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id",        fullUser.get("id"));
        userInfo.put("username",  fullUser.get("username"));
        userInfo.put("role",      fullUser.get("role"));
        userInfo.put("nickname",  fullUser.get("nickname"));
        userInfo.put("avatar",    fullUser.get("avatar"));
        userInfo.put("phone",     fullUser.get("phone"));
        userInfo.put("createdAt", fullUser.get("created_at"));

        Map<String, Object> result = new HashMap<>();
        result.put("token",    token);
        result.put("userInfo", userInfo);

        return Result.success(result);
    }

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");

        if (username == null || username.length() < 3) {
            return Result.error("用户名至少需要 3 位");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码至少需要 6 位");
        }

        String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, username);
        if (count != null && count > 0) {
            return Result.error("用户名已被占用");
        }

        String encodedPassword = passwordEncoder.encode(password);
        jdbcTemplate.update(
                "INSERT INTO users (username, password, role, status) VALUES (?, ?, 0, 1)",
                username, encodedPassword
        );

        return Result.success("注册成功，请登录");
    }
}
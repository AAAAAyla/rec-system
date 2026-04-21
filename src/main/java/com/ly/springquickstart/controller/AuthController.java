// src/main/java/com/ly/springquickstart/controller/AuthController.java
package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");

        // 查询用户（多取一个 role 字段）
        String sql = "SELECT id, username, role FROM users WHERE username = ? AND password = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username, password);

        if (users.isEmpty()) {
            return Result.error("用户名或密码错误");
        }

        Map<String, Object> dbUser = users.get(0);

        // 检查账号状态（status=0 表示封禁）
        String statusSql = "SELECT status FROM users WHERE id = ?";
        Integer status = jdbcTemplate.queryForObject(statusSql, Integer.class, dbUser.get("id"));
        if (status != null && status == 0) {
            return Result.error("账号已被封禁，请联系客服");
        }

        // 生成 JWT，把 id、username、role 都存进去
        Map<String, Object> claims = new HashMap<>();
        claims.put("id",       dbUser.get("id"));
        claims.put("username", dbUser.get("username"));
        claims.put("role",     dbUser.get("role"));
        String token = JwtUtils.genToken(claims);

        // 返回 token + userInfo（前端 userStore 需要）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id",       dbUser.get("id"));
        userInfo.put("username", dbUser.get("username"));
        userInfo.put("role",     dbUser.get("role"));  // 0=买家 1=商家 2=管理员

        Map<String, Object> result = new HashMap<>();
        result.put("token",    token);
        result.put("userInfo", userInfo);

        return Result.success(result);
    }

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");

        // 基础校验
        if (username == null || username.length() < 3) {
            return Result.error("用户名至少需要 3 位");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码至少需要 6 位");
        }

        // 查重
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, username);
        if (count != null && count > 0) {
            return Result.error("用户名已被占用");
        }

        // 注册统一为普通买家 role=0
        jdbcTemplate.update(
                "INSERT INTO users (username, password, role, status) VALUES (?, ?, 0, 1)",
                username, password
        );

        return Result.success("注册成功，请登录");
    }
}
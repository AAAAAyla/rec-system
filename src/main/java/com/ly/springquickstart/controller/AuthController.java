package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
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

        // 去数据库查人
        String sql = "SELECT id, username FROM users WHERE username = ? AND password = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username, password);

        if (!users.isEmpty()) {
            Map<String, Object> dbUser = users.get(0);

            // 准备存入手环的信息
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", dbUser.get("id"));
            claims.put("username", dbUser.get("username"));

            // ⚠️ 使用你现有的 genToken 方法发手环
            String token = JwtUtils.genToken(claims);
            return Result.success(token);
        }
        return Result.error("用户名或密码错误");
    }
    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");

        // 1. 基础检查
        if (username == null || username.length() < 3) return Result.error("用户名至少需要3位");
        if (password == null || password.length() < 6) return Result.error("密码至少需要6位");

        // 2. 查重（避免报错）
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, username);
        if (count != null && count > 0) return Result.error("这个名字太火了，已经被占领了");

        // 3. 存入数据库
        jdbcTemplate.update("INSERT INTO users (username, password) VALUES (?, ?)", username, password);
        return Result.success("账户创建成功！");
    }
}
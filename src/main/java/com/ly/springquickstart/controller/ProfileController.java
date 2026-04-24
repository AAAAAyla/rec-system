package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping
    public Result me() {
        Long userId = uid();
        String sql = "SELECT id, username, nickname, avatar, phone, role FROM users WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        if (rows.isEmpty()) return Result.error("用户不存在");
        return Result.success(rows.get(0));
    }

    @PutMapping
    public Result update(@RequestBody Map<String, String> body) {
        Long userId  = uid();
        String nick  = body.get("nickname");
        String avatar = body.get("avatar");
        String phone  = body.get("phone");

        jdbcTemplate.update(
            "UPDATE users SET nickname=COALESCE(?,nickname), avatar=COALESCE(?,avatar), phone=COALESCE(?,phone) WHERE id=?",
            nick, avatar, phone, userId
        );
        return Result.success("更新成功");
    }

    @PutMapping("/password")
    public Result changePassword(@RequestBody Map<String, String> body) {
        Long userId     = uid();
        String oldPwd   = body.get("oldPassword");
        String newPwd   = body.get("newPassword");

        if (newPwd == null || newPwd.length() < 6) {
            return Result.error("新密码至少需要 6 位");
        }

        String storedPwd = jdbcTemplate.queryForObject(
            "SELECT password FROM users WHERE id=?", String.class, userId);

        boolean match;
        if (storedPwd.startsWith("$2a$") || storedPwd.startsWith("$2b$")) {
            match = passwordEncoder.matches(oldPwd, storedPwd);
        } else {
            match = oldPwd.equals(storedPwd);
        }
        if (!match) return Result.error("原密码错误");

        jdbcTemplate.update("UPDATE users SET password=? WHERE id=?",
                passwordEncoder.encode(newPwd), userId);
        return Result.success("密码修改成功，请重新登录");
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }
}

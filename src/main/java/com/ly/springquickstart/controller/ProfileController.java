package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户个人中心接口
 * 依赖 users 表（id, username, password, nickname, avatar, phone, role, status）
 * 若 users 表缺少 nickname / avatar / phone 列，执行以下 SQL 补充：
 *   ALTER TABLE users ADD COLUMN nickname VARCHAR(50) NULL;
 *   ALTER TABLE users ADD COLUMN avatar   VARCHAR(255) NULL;
 *   ALTER TABLE users ADD COLUMN phone    VARCHAR(20) NULL;
 */
@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 查看个人信息 */
    @GetMapping
    public Result me() {
        Long userId = uid();
        String sql = "SELECT id, username, nickname, avatar, phone, role FROM users WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        if (rows.isEmpty()) return Result.error("用户不存在");
        return Result.success(rows.get(0));
    }

    /**
     * 更新个人信息（昵称、头像、手机号）
     * body: { "nickname": "小明", "avatar": "https://...", "phone": "13800138000" }
     */
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

    /**
     * 修改密码
     * body: { "oldPassword": "xxx", "newPassword": "yyy" }
     */
    @PutMapping("/password")
    public Result changePassword(@RequestBody Map<String, String> body) {
        Long userId     = uid();
        String oldPwd   = body.get("oldPassword");
        String newPwd   = body.get("newPassword");

        if (newPwd == null || newPwd.length() < 6) {
            return Result.error("新密码至少需要 6 位");
        }

        // 查旧密码（支持明文和 MD5 两种存储格式）
        String storedPwd = jdbcTemplate.queryForObject(
            "SELECT password FROM users WHERE id=?", String.class, userId);

        boolean match = oldPwd.equals(storedPwd)
                || md5(oldPwd).equals(storedPwd);
        if (!match) return Result.error("原密码错误");

        jdbcTemplate.update("UPDATE users SET password=? WHERE id=?", md5(newPwd), userId);
        return Result.success("密码修改成功，请重新登录");
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }

    private String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }
}

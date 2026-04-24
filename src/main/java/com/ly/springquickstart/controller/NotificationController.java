package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired private JdbcTemplate jdbcTemplate;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = uid();
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC LIMIT ?,?",
                userId, offset, pageSize);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id=?", Integer.class, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        return Result.success(data);
    }

    @GetMapping("/unread-count")
    public Result unreadCount() {
        Long userId = uid();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0", Integer.class, userId);
        return Result.success(count);
    }

    @PutMapping("/{id}/read")
    public Result markRead(@PathVariable Long id) {
        Long userId = uid();
        jdbcTemplate.update("UPDATE notifications SET is_read=1 WHERE id=? AND user_id=?", id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result markAllRead() {
        Long userId = uid();
        jdbcTemplate.update("UPDATE notifications SET is_read=1 WHERE user_id=? AND is_read=0", userId);
        return Result.success();
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }
}

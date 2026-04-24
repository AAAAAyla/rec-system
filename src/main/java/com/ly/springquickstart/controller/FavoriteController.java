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
@RequestMapping("/favorites")
public class FavoriteController {

    @Autowired private JdbcTemplate jdbcTemplate;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = uid();
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT f.id, f.item_id, f.created_at, i.title, i.price, i.image_url " +
                "FROM favorites f JOIN items i ON f.item_id = i.id " +
                "WHERE f.user_id = ? ORDER BY f.created_at DESC LIMIT ?,?",
                userId, offset, pageSize);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM favorites WHERE user_id=?", Integer.class, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        return Result.success(data);
    }

    @PostMapping("/{itemId}")
    public Result add(@PathVariable Long itemId) {
        Long userId = uid();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM favorites WHERE user_id=? AND item_id=?", Integer.class, userId, itemId);
        if (count != null && count > 0) return Result.error("已收藏");
        jdbcTemplate.update("INSERT INTO favorites(user_id, item_id) VALUES(?,?)", userId, itemId);
        return Result.success();
    }

    @DeleteMapping("/{itemId}")
    public Result remove(@PathVariable Long itemId) {
        Long userId = uid();
        jdbcTemplate.update("DELETE FROM favorites WHERE user_id=? AND item_id=?", userId, itemId);
        return Result.success();
    }

    @GetMapping("/check/{itemId}")
    public Result check(@PathVariable Long itemId) {
        Long userId = uid();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM favorites WHERE user_id=? AND item_id=?", Integer.class, userId, itemId);
        return Result.success(count != null && count > 0);
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }
}

package com.ly.springquickstart.controller;

import com.ly.springquickstart.annotation.RoleRequired;
import com.ly.springquickstart.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired private JdbcTemplate jdbcTemplate;

    /** 用户列表 */
    @GetMapping("/users")
    @RoleRequired({2})
    public Result users(@RequestParam(required = false) String kw,
                        @RequestParam(defaultValue = "1") int pageNum,
                        @RequestParam(defaultValue = "20") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        String where = "WHERE 1=1";
        if (kw != null && !kw.isEmpty()) where += " AND (username LIKE '%" + kw + "%' OR phone LIKE '%" + kw + "%')";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, nickname, phone, role, status, created_at FROM users " + where +
                " ORDER BY id DESC LIMIT ?,?", offset, pageSize);
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users " + where, Integer.class);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        return Result.success(data);
    }

    /** 封禁/解封用户 */
    @PutMapping("/users/{id}/status")
    @RoleRequired({2})
    public Result userStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        int status = body.getOrDefault("status", 1);
        jdbcTemplate.update("UPDATE users SET status=? WHERE id=?", status, id);
        return Result.success();
    }

    /** 商品审核列表 */
    @GetMapping("/items")
    @RoleRequired({2})
    public Result items(@RequestParam(defaultValue = "2") int status,
                        @RequestParam(defaultValue = "1") int pageNum,
                        @RequestParam(defaultValue = "20") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT i.*, m.shop_name FROM items i LEFT JOIN merchants m ON i.merchant_id=m.id " +
                "WHERE i.status=? ORDER BY i.create_time DESC LIMIT ?,?",
                status, offset, pageSize);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM items WHERE status=?", Integer.class, status);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        return Result.success(data);
    }

    /** 审核商品 */
    @PutMapping("/items/{id}/audit")
    @RoleRequired({2})
    public Result auditItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int status = Integer.parseInt(body.getOrDefault("status", 1).toString());
        jdbcTemplate.update("UPDATE items SET status=? WHERE id=?", status, id);
        return Result.success();
    }

    /** 全平台订单列表 */
    @GetMapping("/orders")
    @RoleRequired({2})
    public Result orders(@RequestParam(required = false) Integer status,
                         @RequestParam(defaultValue = "1") int pageNum,
                         @RequestParam(defaultValue = "20") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        String where = status != null ? "WHERE o.status=" + status : "";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT o.*, u.username AS buyer_name, m.shop_name FROM orders o " +
                "LEFT JOIN users u ON o.user_id=u.id LEFT JOIN merchants m ON o.merchant_id=m.id " +
                where + " ORDER BY o.create_time DESC LIMIT ?,?", offset, pageSize);
        String countWhere = status != null ? "WHERE status=" + status : "";
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders " + countWhere, Integer.class);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        return Result.success(data);
    }

    /** 平台统计数据 */
    @GetMapping("/stats")
    @RoleRequired({2})
    public Result stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
        stats.put("totalMerchants", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants WHERE status=1", Integer.class));
        stats.put("totalItems", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM items WHERE status=1", Integer.class));
        stats.put("totalOrders", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
        stats.put("totalGMV", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(pay_amount),0) FROM orders WHERE status >= 1", BigDecimal.class));
        stats.put("todayOrders", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE DATE(create_time)=CURDATE()", Integer.class));
        stats.put("todayGMV", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(pay_amount),0) FROM orders WHERE status>=1 AND DATE(create_time)=CURDATE()", BigDecimal.class));
        return Result.success(stats);
    }
}

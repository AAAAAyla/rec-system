package com.ly.springquickstart.controller;

import com.ly.springquickstart.annotation.RoleRequired;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.MerchantService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MerchantService merchantService;

    /** 商家创建优惠券 */
    @PostMapping
    @RoleRequired({1})
    public Result create(@RequestBody Map<String, Object> body) {
        Long merchantId = getMerchantId();
        jdbcTemplate.update(
                "INSERT INTO coupons(merchant_id, type, title, discount, min_amount, total_count, remain_count, expire_time) " +
                "VALUES(?,?,?,?,?,?,?,?)",
                merchantId, body.get("type"), body.get("title"), body.get("discount"),
                body.get("minAmount"), body.get("totalCount"), body.get("totalCount"), body.get("expireTime"));
        return Result.success();
    }

    /** 商家查看自己的优惠券列表 */
    @GetMapping("/merchant")
    @RoleRequired({1})
    public Result merchantList() {
        Long merchantId = getMerchantId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM coupons WHERE merchant_id=? ORDER BY created_at DESC", merchantId);
        return Result.success(rows);
    }

    /** 买家可领取的优惠券列表 */
    @GetMapping("/available")
    public Result available(@RequestParam(required = false) Long merchantId) {
        String sql = "SELECT * FROM coupons WHERE remain_count > 0 AND expire_time > NOW()";
        if (merchantId != null) sql += " AND merchant_id = " + merchantId;
        sql += " ORDER BY created_at DESC";
        return Result.success(jdbcTemplate.queryForList(sql));
    }

    /** 买家领取优惠券 */
    @PostMapping("/{couponId}/claim")
    public Result claim(@PathVariable Long couponId) {
        Long userId = uid();
        Integer taken = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_coupons WHERE user_id=? AND coupon_id=?", Integer.class, userId, couponId);
        if (taken != null && taken > 0) return Result.error("已领取过该优惠券");

        int rows = jdbcTemplate.update(
                "UPDATE coupons SET remain_count = remain_count - 1 WHERE id=? AND remain_count > 0", couponId);
        if (rows == 0) return Result.error("优惠券已被领完");

        jdbcTemplate.update(
                "INSERT INTO user_coupons(user_id, coupon_id, status) VALUES(?,?,'unused')", userId, couponId);

        try {
            Map<String, Object> coupon = jdbcTemplate.queryForMap("SELECT title FROM coupons WHERE id=?", couponId);
            jdbcTemplate.update(
                    "INSERT INTO notifications(user_id, title, content, type) VALUES(?,?,?,?)",
                    userId, "优惠券领取成功", "您成功领取了优惠券「" + coupon.get("title") + "」，快去使用吧！", "coupon");
        } catch (Exception ignored) {}

        return Result.success("领取成功");
    }

    /** 买家查看自己的优惠券 */
    @GetMapping("/mine")
    public Result mine(@RequestParam(defaultValue = "unused") String status) {
        Long userId = uid();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT uc.id, uc.status, c.title, c.type, c.discount, c.min_amount, c.expire_time, c.merchant_id " +
                "FROM user_coupons uc JOIN coupons c ON uc.coupon_id = c.id " +
                "WHERE uc.user_id=? AND uc.status=? ORDER BY c.expire_time ASC",
                userId, status);
        return Result.success(rows);
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }

    private Long getMerchantId() {
        return merchantService.getMerchantIdByUser(uid());
    }
}

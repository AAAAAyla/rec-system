package com.ly.springquickstart.controller;

import com.ly.springquickstart.annotation.RoleRequired;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.MerchantService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/merchant/dashboard")
public class DashboardController {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MerchantService merchantService;

    @GetMapping("/stats")
    @RoleRequired({1})
    public Result stats() {
        Long merchantId = getMerchantId();
        Map<String, Object> stats = new HashMap<>();

        Integer todayOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE merchant_id=? AND DATE(create_time)=CURDATE()", Integer.class, merchantId);
        BigDecimal todayRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(pay_amount),0) FROM orders WHERE merchant_id=? AND status>=1 AND DATE(create_time)=CURDATE()",
                BigDecimal.class, merchantId);
        Integer monthOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE merchant_id=? AND YEAR(create_time)=YEAR(CURDATE()) AND MONTH(create_time)=MONTH(CURDATE())",
                Integer.class, merchantId);
        BigDecimal monthRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(pay_amount),0) FROM orders WHERE merchant_id=? AND status>=1 AND YEAR(create_time)=YEAR(CURDATE()) AND MONTH(create_time)=MONTH(CURDATE())",
                BigDecimal.class, merchantId);
        Integer onSaleItems = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM items WHERE merchant_id=? AND status=1", Integer.class, merchantId);
        Integer pendingOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE merchant_id=? AND status=1", Integer.class, merchantId);

        stats.put("todayOrders", todayOrders);
        stats.put("todayRevenue", todayRevenue);
        stats.put("monthOrders", monthOrders);
        stats.put("monthRevenue", monthRevenue);
        stats.put("onSaleItems", onSaleItems);
        stats.put("pendingOrders", pendingOrders);

        // 分类饼图数据
        try {
            stats.put("categoryDistribution", jdbcTemplate.queryForList(
                    """
                    SELECT COALESCE(c.name,'未分类') AS name, COUNT(*) AS value
                    FROM items i LEFT JOIN categories c ON i.category_id = c.id
                    WHERE i.merchant_id = ? AND i.status = 1
                    GROUP BY c.name ORDER BY value DESC LIMIT 8
                    """, merchantId));
        } catch (Exception e) { stats.put("categoryDistribution", List.of()); }

        // 订单状态饼图数据
        try {
            stats.put("orderStatusDistribution", jdbcTemplate.queryForList(
                    """
                    SELECT CASE status
                      WHEN 0 THEN '待付款' WHEN 1 THEN '待发货' WHEN 2 THEN '已发货'
                      WHEN 3 THEN '已完成' WHEN 4 THEN '已取消' WHEN 5 THEN '退款中'
                      ELSE '其他' END AS name, COUNT(*) AS value
                    FROM orders WHERE merchant_id = ?
                    GROUP BY status ORDER BY status
                    """, merchantId));
        } catch (Exception e) { stats.put("orderStatusDistribution", List.of()); }

        return Result.success(stats);
    }

    private Long getMerchantId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = Long.parseLong(claims.get("id").toString());
        return merchantService.getMerchantIdByUser(userId);
    }
}

package com.ly.springquickstart.controller;

import com.ly.springquickstart.annotation.RoleRequired;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.MerchantService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MerchantService merchantService;

    @GetMapping
    @RoleRequired({1})
    public Result list() {
        Long merchantId = getMerchantId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM warehouses WHERE merchant_id = ? ORDER BY is_default DESC, id DESC", merchantId);
        return Result.success(rows);
    }

    @PostMapping
    @RoleRequired({1})
    public Result add(@RequestBody Map<String, Object> body) {
        Long merchantId = getMerchantId();
        jdbcTemplate.update(
                "INSERT INTO warehouses(merchant_id, name, phone, province, city, district, detail, is_default) VALUES(?,?,?,?,?,?,?,0)",
                merchantId, body.get("name"), body.get("phone"),
                body.get("province"), body.get("city"), body.get("district"), body.get("detail"));
        return Result.success();
    }

    @PutMapping("/{id}")
    @RoleRequired({1})
    public Result update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long merchantId = getMerchantId();
        jdbcTemplate.update(
                "UPDATE warehouses SET name=?, phone=?, province=?, city=?, district=?, detail=? WHERE id=? AND merchant_id=?",
                body.get("name"), body.get("phone"),
                body.get("province"), body.get("city"), body.get("district"), body.get("detail"),
                id, merchantId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RoleRequired({1})
    public Result delete(@PathVariable Long id) {
        Long merchantId = getMerchantId();
        jdbcTemplate.update("DELETE FROM warehouses WHERE id=? AND merchant_id=?", id, merchantId);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    @RoleRequired({1})
    public Result setDefault(@PathVariable Long id) {
        Long merchantId = getMerchantId();
        jdbcTemplate.update("UPDATE warehouses SET is_default=0 WHERE merchant_id=?", merchantId);
        jdbcTemplate.update("UPDATE warehouses SET is_default=1 WHERE id=? AND merchant_id=?", id, merchantId);
        return Result.success();
    }

    private Long getMerchantId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Long userId = Long.parseLong(claims.get("id").toString());
        return merchantService.getMerchantIdByUser(userId);
    }
}

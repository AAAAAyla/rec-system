package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.CartService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购物车接口（基于 Redis，数据不落库）
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /** 查看购物车 */
    @GetMapping
    public Result list() {
        return Result.success(cartService.list(uid()));
    }

    /**
     * 加入购物车
     * body: { "itemId":1, "skuId":2, "quantity":1 }
     */
    @PostMapping
    public Result add(@RequestBody Map<String, Object> body) {
        Long itemId   = toLong(body.get("itemId"));
        Long skuId    = toLong(body.get("skuId"));
        Integer qty   = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 1;
        cartService.add(uid(), itemId, skuId, qty);
        return Result.success("已加入购物车");
    }

    /**
     * 修改数量
     * body: { "itemId":1, "skuId":2, "quantity":3 }
     */
    @PutMapping("/quantity")
    public Result updateQuantity(@RequestBody Map<String, Object> body) {
        Long itemId = toLong(body.get("itemId"));
        Long skuId  = toLong(body.get("skuId"));
        int qty     = Integer.parseInt(body.get("quantity").toString());
        cartService.updateQuantity(uid(), itemId, skuId, qty);
        return Result.success();
    }

    /**
     * 切换单条选中状态
     * body: { "itemId":1, "skuId":2, "checked":true }
     */
    @PutMapping("/check")
    public Result toggleChecked(@RequestBody Map<String, Object> body) {
        Long itemId    = toLong(body.get("itemId"));
        Long skuId     = toLong(body.get("skuId"));
        Boolean checked = Boolean.parseBoolean(body.get("checked").toString());
        cartService.toggleChecked(uid(), itemId, skuId, checked);
        return Result.success();
    }

    /**
     * 全选 / 取消全选
     * body: { "checked": true }
     */
    @PutMapping("/check/all")
    public Result checkAll(@RequestBody Map<String, Object> body) {
        Boolean checked = Boolean.parseBoolean(body.get("checked").toString());
        cartService.checkAll(uid(), checked);
        return Result.success();
    }

    /**
     * 删除单条
     * body: { "itemId":1, "skuId":2 }
     */
    @DeleteMapping
    public Result remove(@RequestBody Map<String, Object> body) {
        Long itemId = toLong(body.get("itemId"));
        Long skuId  = toLong(body.get("skuId"));
        cartService.remove(uid(), itemId, skuId);
        return Result.success();
    }

    /** 清空购物车 */
    @DeleteMapping("/clear")
    public Result clear() {
        cartService.clear(uid());
        return Result.success("购物车已清空");
    }

    /** 查看选中商品总价 */
    @GetMapping("/total")
    public Result total() {
        return Result.success(Map.of("total", cartService.calcCheckedTotal(uid())));
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        return Long.parseLong(val.toString());
    }
}

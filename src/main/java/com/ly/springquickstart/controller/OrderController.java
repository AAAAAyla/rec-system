package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Address;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.AddressService;
import com.ly.springquickstart.service.CartService;
import com.ly.springquickstart.service.MerchantService;
import com.ly.springquickstart.service.OrderService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired private OrderService   orderService;
    @Autowired private AddressService addressService;
    @Autowired private MerchantService merchantService;
    @Autowired private CartService    cartService;

    // ── 买家接口 ───────────────────────────────────────

    /**
     * 创建订单
     * body: {
     *   "addressId": 1,
     *   "remark": "...",
     *   "cartItems": [
     *     { "itemId":1, "skuId":2, "quantity":1, "title":"...", "imageUrl":"...", "specJson":"..." }
     *   ]
     * }
     */
    @PostMapping
    public Result create(@RequestBody Map<String, Object> body) {
        Long userId    = uid();
        Long addressId = toLong(body.get("addressId"));
        String remark  = (String) body.getOrDefault("remark", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) body.get("cartItems");

        Address address = addressService.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            return Result.error("收货地址不存在");
        }

        String orderNo = orderService.createOrder(userId, addressId, cartItems, remark, address);
        cartService.removeChecked(userId);
        return Result.success(Map.of("orderNo", orderNo));
    }

    /**
     * 模拟支付：body: { "orderNo": "xxx" }
     */
    @PostMapping("/pay")
    public Result mockPay(@RequestBody Map<String, String> body) {
        orderService.mockPay(uid(), body.get("orderNo"));
        return Result.success("支付成功");
    }

    /** 买家订单列表 */
    @GetMapping
    public Result list(@RequestParam(required = false) Integer status,
                       @RequestParam(defaultValue = "1")  int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.listByUser(uid(), status, pageNum, pageSize));
    }

    /** 订单详情（含物流） */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        return Result.success(orderService.detail(uid(), id));
    }

    /** 买家取消订单 */
    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.cancelByUser(uid(), id, body.getOrDefault("reason", "买家主动取消"));
        return Result.success();
    }

    /** 买家确认收货 */
    @PutMapping("/{id}/confirm")
    public Result confirm(@PathVariable Long id) {
        orderService.confirmReceive(uid(), id);
        return Result.success();
    }

    /** 买家查询物流详情 */
    @GetMapping("/{id}/logistics")
    public Result logistics(@PathVariable Long id) {
        return Result.success(orderService.getLogistics(uid(), id));
    }

    /** 申请退款 */
    @PostMapping("/{id}/refund")
    public Result refund(@PathVariable Long id) {
        orderService.applyRefund(uid(), id);
        return Result.success("退款申请已提交");
    }

    // ── 商家接口 ───────────────────────────────────────

    /** 商家订单列表 */
    @GetMapping("/merchant")
    public Result merchantList(@RequestParam(required = false) Integer status,
                               @RequestParam(defaultValue = "1")  int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize) {
        Long merchantId = merchantService.getMerchantIdByUser(uid());
        return Result.success(orderService.listByMerchant(merchantId, status, pageNum, pageSize));
    }

    /**
     * 商家发货
     * body: { "expressCompany": "顺丰", "trackingNo": "SF123456789" }
     */
    @PutMapping("/{id}/ship")
    public Result ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long merchantId = merchantService.getMerchantIdByUser(uid());
        orderService.ship(merchantId, id, body.get("expressCompany"), body.get("trackingNo"));
        return Result.success();
    }

    /**
     * 商家追加物流节点
     * body: { "location": "北京分拣中心", "description": "包裹已到达北京分拣中心" }
     */
    @PostMapping("/{id}/track")
    public Result addTrack(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long merchantId = merchantService.getMerchantIdByUser(uid());
        orderService.addTrack(merchantId, id, body.get("location"), body.get("description"));
        return Result.success("物流节点已添加");
    }

    /** 商家同意退款 */
    @PutMapping("/{id}/refund/agree")
    public Result agreeRefund(@PathVariable Long id) {
        Long merchantId = merchantService.getMerchantIdByUser(uid());
        orderService.agreeRefund(merchantId, id);
        return Result.success();
    }

    // ── 工具 ──────────────────────────────────────────
    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        return Long.parseLong(val.toString());
    }
}

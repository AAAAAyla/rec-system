package com.ly.springquickstart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ly.springquickstart.mapper.ItemMapper;
import com.ly.springquickstart.mapper.OrderMapper;
import com.ly.springquickstart.mapper.ShipmentMapper;
import com.ly.springquickstart.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    @Autowired private OrderMapper      orderMapper;
    @Autowired private ItemMapper       itemMapper;
    @Autowired private ShipmentMapper   shipmentMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ObjectMapper     objectMapper;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // Redis key 前缀：未付款订单超时取消
    private static final String ORDER_EXPIRE_KEY = "order:expire:";
    // 30 分钟未付款自动取消
    private static final long   ORDER_TTL_MINUTES = 30;

    // ── 创建订单 ──────────────────────────────────────

    /**
     * 创建订单（含原子库存扣减）
     *
     * @param userId    买家 ID
     * @param addressId 收货地址 ID
     * @param cartItems 购物车选中项 List<Map> 每项含 itemId / skuId / quantity
     * @param remark    买家备注
     * @return 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long userId, Long addressId,
                               List<Map<String, Object>> cartItems, String remark,
                               Address address) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("购物车不能为空");
        }

        // 1. 生成唯一订单号
        String orderNo = generateOrderNo();

        // 2. 构建地址快照（JSON）
        String addressSnapshot = toJson(address);

        // 3. 遍历购物车，逐 SKU 扣库存（乐观锁）
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Long merchantId = null;

        for (Map<String, Object> cartItem : cartItems) {
            Long   skuId    = toLong(cartItem.get("skuId"));
            Long   itemId   = toLong(cartItem.get("itemId"));
            int    quantity = toInt(cartItem.get("quantity"));
            String title    = (String) cartItem.get("title");
            String imageUrl = (String) cartItem.get("imageUrl");
            String specJson = (String) cartItem.get("specJson");

            // 查 SKU 价格（兼容无 SKU 商品）
            ItemSku sku;
            if (skuId != null) {
                sku = itemMapper.findSkuById(skuId);
                if (sku == null) throw new RuntimeException("SKU 不存在：" + skuId);
            } else {
                // 没有指定 SKU，尝试取商品第一个在售 SKU
                List<ItemSku> availableSkus = itemMapper.findSkusByItemId(itemId);
                if (!availableSkus.isEmpty()) {
                    sku   = availableSkus.get(0);
                    skuId = sku.getId();
                } else {
                    // 商品未配置 SKU，用商品主表价格/库存
                    Item fallbackItem = itemMapper.findById(itemId);
                    sku = new ItemSku();
                    sku.setId(null);
                    sku.setPrice(fallbackItem.getPrice() != null ? fallbackItem.getPrice() : BigDecimal.ZERO);
                    sku.setStock(fallbackItem.getStock() != null ? fallbackItem.getStock() : 9999);
                }
            }

            if (sku.getStock() < quantity) throw new RuntimeException("库存不足：" + title);

            // 有 SKU 才做原子扣减
            if (skuId != null) {
                int affected = itemMapper.deductSkuStock(skuId, quantity);
                if (affected == 0) throw new RuntimeException("库存不足（并发）：" + title);
            }

            // 查商品获取 merchantId（同一订单要求同一商家，跨商家需拆单）
            Item item = itemMapper.findById(itemId);
            if (item == null) throw new RuntimeException("商品不存在");
            if (merchantId == null) {
                merchantId = item.getMerchantId();
            } else if (!merchantId.equals(item.getMerchantId())) {
                throw new RuntimeException("暂不支持跨商家下单，请分开结算");
            }

            BigDecimal lineTotal = sku.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);

            OrderItem oi = new OrderItem();
            oi.setItemId(itemId);
            oi.setSkuId(skuId);
            oi.setTitle(title != null ? title : item.getTitle());
            oi.setImageUrl(imageUrl != null ? imageUrl : item.getImageUrl());
            oi.setSpecJson(specJson != null ? specJson : sku.getSpecJson());
            oi.setPrice(sku.getPrice());
            oi.setQuantity(quantity);
            orderItems.add(oi);
        }

        // 4. 写入订单主表
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setStatus(0);                          // 待付款
        order.setTotalAmount(total);
        order.setFreightAmount(BigDecimal.ZERO);     // MVP 免运费
        order.setPayAmount(total);
        order.setAddressSnapshot(addressSnapshot);
        order.setRemark(remark);
        orderMapper.insert(order);

        // 5. 写入订单明细
        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderMapper.insertItem(oi);
            // 累计销量
            itemMapper.incrSales(oi.getItemId(), oi.getQuantity());
        }

        // 6. Redis 写入超时 key（30分钟后可被定时任务/延迟队列触发取消）
        redisTemplate.opsForValue().set(
            ORDER_EXPIRE_KEY + orderNo,
            String.valueOf(order.getId()),
            ORDER_TTL_MINUTES, TimeUnit.MINUTES
        );

        return orderNo;
    }

    // ── 模拟支付 ──────────────────────────────────────

    /**
     * 模拟支付：直接将订单状态从 0→1（待发货）
     */
    @Transactional
    public void mockPay(Long userId, String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null)              throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new RuntimeException("无权操作");
        if (order.getStatus() != 0)    throw new RuntimeException("订单状态不正确");

        int rows = orderMapper.markPaid(order.getId(), 1);
        if (rows == 0) throw new RuntimeException("支付失败，可能订单已取消");

        // 删除超时 key（已付款无需再取消）
        redisTemplate.delete(ORDER_EXPIRE_KEY + orderNo);
    }

    // ── 买家操作 ──────────────────────────────────────

    public Map<String, Object> listByUser(Long userId, Integer status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Order> rows = orderMapper.findByUser(userId, status, offset, pageSize);
        // 批量组装明细
        rows.forEach(o -> o.setItems(orderMapper.findItemsByOrderId(o.getId())));
        int total = orderMapper.countByUser(userId, status);
        return Map.of("rows", rows, "total", total);
    }

    public Order detail(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getUserId().equals(userId))
            throw new RuntimeException("订单不存在");
        order.setItems(orderMapper.findItemsByOrderId(orderId));
        Shipment shipment = shipmentMapper.findByOrderId(orderId);
        if (shipment != null) {
            shipment.setTracks(shipmentMapper.findTracks(shipment.getId()));
            order.setShipment(shipment);
        }
        return order;
    }

    /** 买家主动取消（仅限待付款状态） */
    @Transactional
    public void cancelByUser(Long userId, Long orderId, String reason) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getUserId().equals(userId))
            throw new RuntimeException("订单不存在");
        int rows = orderMapper.cancel(orderId, 0, reason);
        if (rows == 0) throw new RuntimeException("只有待付款订单可以取消");
        // 回滚库存
        rollbackStock(orderId);
        redisTemplate.delete(ORDER_EXPIRE_KEY + order.getOrderNo());
    }

    /** 买家确认收货（2→3） */
    @Transactional
    public void confirmReceive(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getUserId().equals(userId))
            throw new RuntimeException("订单不存在");
        int rows = orderMapper.compareAndSetStatus(orderId, 2, 3);
        if (rows == 0) throw new RuntimeException("订单状态不正确");
    }

    // ── 商家操作 ──────────────────────────────────────

    public Map<String, Object> listByMerchant(Long merchantId, Integer status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Order> rows = orderMapper.findByMerchant(merchantId, status, offset, pageSize);
        rows.forEach(o -> o.setItems(orderMapper.findItemsByOrderId(o.getId())));
        int total = orderMapper.countByMerchant(merchantId, status);
        return Map.of("rows", rows, "total", total);
    }

    /** 商家发货 */
    @Transactional
    public void ship(Long merchantId, Long orderId, String expressCompany, String trackingNo) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId))
            throw new RuntimeException("订单不存在");
        int rows = orderMapper.compareAndSetStatus(orderId, 1, 2);
        if (rows == 0) throw new RuntimeException("只有待发货订单可以发货");

        // 写物流主记录
        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setExpressCompany(expressCompany);
        shipment.setTrackingNo(trackingNo);
        shipmentMapper.insert(shipment);

        // 写第一条节点
        ShipmentTrack track = new ShipmentTrack();
        track.setShipmentId(shipment.getId());
        track.setDescription("商家已发货，等待揽收");
        track.setTrackTime(LocalDateTime.now());
        shipmentMapper.insertTrack(track);

        // 通知买家
        try {
            String company = expressCompany != null ? expressCompany : "快递";
            String trackNo = trackingNo != null ? trackingNo : "";
            jdbcTemplate.update(
                    "INSERT INTO notifications(user_id, title, content, type) VALUES(?,?,?,?)",
                    order.getUserId(),
                    "订单已发货",
                    "您的订单已由" + company + "发出，运单号：" + trackNo + "，请注意查收！",
                    "shipping");
        } catch (Exception ignored) {}
    }

    // ── 物流追踪 ──────────────────────────────────────

    /**
     * 商家手动追加物流节点
     */
    @Transactional
    public void addTrack(Long merchantId, Long orderId, String location, String description) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId))
            throw new RuntimeException("订单不存在");
        if (order.getStatus() != 2)
            throw new RuntimeException("订单尚未发货，无法追加物流节点");

        Shipment shipment = shipmentMapper.findByOrderId(orderId);
        if (shipment == null) throw new RuntimeException("物流记录不存在");

        ShipmentTrack track = new ShipmentTrack();
        track.setShipmentId(shipment.getId());
        track.setLocation(location);
        track.setDescription(description);
        track.setTrackTime(LocalDateTime.now());
        shipmentMapper.insertTrack(track);
    }

    /**
     * 买家查询物流详情（快递公司 + 单号 + 全部节点）
     */
    public Map<String, Object> getLogistics(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getUserId().equals(userId))
            throw new RuntimeException("订单不存在");

        Shipment shipment = shipmentMapper.findByOrderId(orderId);
        if (shipment == null) {
            throw new RuntimeException("该订单暂无物流信息");
        }
        List<ShipmentTrack> tracks = shipmentMapper.findTracks(shipment.getId());
        shipment.setTracks(tracks);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("orderStatus", order.getStatus());
        result.put("expressCompany", shipment.getExpressCompany());
        result.put("trackingNo", shipment.getTrackingNo());
        result.put("shipTime", shipment.getShipTime());
        result.put("tracks", tracks);
        return result;
    }

    /** 商家申请退款同意（5→6） */
    @Transactional
    public void agreeRefund(Long merchantId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId))
            throw new RuntimeException("订单不存在");
        int rows = orderMapper.compareAndSetStatus(orderId, 5, 6);
        if (rows == 0) throw new RuntimeException("状态不正确");
        rollbackStock(orderId);
    }

    /** 买家申请退款（1或2→5） */
    @Transactional
    public void applyRefund(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getUserId().equals(userId))
            throw new RuntimeException("订单不存在");
        if (order.getStatus() != 1 && order.getStatus() != 2)
            throw new RuntimeException("当前状态不支持退款");
        orderMapper.compareAndSetStatus(orderId, order.getStatus(), 5);
    }

    // ── 超时自动取消（定时任务调用）──────────────────

    /**
     * 扫描 Redis 过期 key 并取消超时订单
     * 实际项目用 Redisson DelayQueue 或 Spring @Scheduled 扫库
     * MVP 用 @Scheduled 每分钟扫一次待付款订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoCancelExpired() {
        List<Order> expired = orderMapper.findExpiredUnpaid(ORDER_TTL_MINUTES);
        for (Order order : expired) {
            try {
                int rows = orderMapper.cancel(order.getId(), 0, "超时未付款，系统自动取消");
                if (rows > 0) {
                    rollbackStock(order.getId());
                    redisTemplate.delete(ORDER_EXPIRE_KEY + order.getOrderNo());
                    System.out.println("[定时任务] 已取消超时订单: " + order.getOrderNo());
                }
            } catch (Exception e) {
                System.err.println("[定时任务] 取消订单异常: " + order.getOrderNo() + " - " + e.getMessage());
            }
        }
    }

    // ── 私有工具 ──────────────────────────────────────

    private void rollbackStock(Long orderId) {
        List<OrderItem> items = orderMapper.findItemsByOrderId(orderId);
        for (OrderItem oi : items) {
            if (oi.getSkuId() != null) {
                itemMapper.restoreSkuStock(oi.getSkuId(), oi.getQuantity());
            }
        }
    }

    private String generateOrderNo() {
        return System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 9999));
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        return Long.parseLong(val.toString());
    }

    private int toInt(Object val) {
        if (val == null) return 1;
        return Integer.parseInt(val.toString());
    }
}

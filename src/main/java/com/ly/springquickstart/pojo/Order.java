// src/main/java/com/ly/springquickstart/pojo/Order.java
package com.ly.springquickstart.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    /**
     * 0=待付款 1=待发货 2=已发货 3=已完成 4=已取消 5=退款中 6=已退款
     */
    private Integer status;
    private BigDecimal totalAmount;
    private BigDecimal freightAmount;
    private BigDecimal payAmount;
    /** 下单时收货地址快照（JSON字符串） */
    private String addressSnapshot;
    private String remark;
    private String cancelReason;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 非数据库列，查询后手动组装
    private List<OrderItem> orderItems;
    private Shipment shipment;

    public void setItems(List<OrderItem> itemsByOrderId) {
    }
}
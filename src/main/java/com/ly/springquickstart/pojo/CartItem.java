package com.ly.springquickstart.pojo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 购物车条目（存储于 Redis，不落库）
 */
@Data
public class CartItem {
    private Long itemId;
    private Long skuId;
    private String title;
    private String imageUrl;
    private String specJson;
    private BigDecimal price;
    private Integer quantity;
    /** 是否选中（结算时用） */
    private Boolean checked = true;
}

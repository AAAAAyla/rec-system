// src/main/java/com/ly/springquickstart/pojo/OrderItem.java
package com.ly.springquickstart.pojo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long itemId;
    private Long skuId;
    /** 下单时商品名称快照 */
    private String title;
    /** 下单时主图快照 */
    private String imageUrl;
    /** 下单时规格快照，如 {"颜色":"红","尺寸":"M"} */
    private String specJson;
    /** 下单时单价快照 */
    private BigDecimal price;
    private Integer quantity;
}

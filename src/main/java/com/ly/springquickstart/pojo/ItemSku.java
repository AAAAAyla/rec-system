package com.ly.springquickstart.pojo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemSku {
    private Long id;
    private Long itemId;
    /** 规格JSON字符串，如 {"颜色":"红","尺寸":"M"} */
    private String specJson;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    /** 1=正常 0=下架 */
    private Integer status;
}
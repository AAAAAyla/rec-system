package com.ly.springquickstart.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ── 商品主体 ──────────────────────────────────────────
@Data
public class Item {
    private Long id;
    private Long merchantId;
    private Integer categoryId;
    private String title;
    private String description;
    private String imageUrl;        // 主图（兼容旧字段）
    /** 0=下架 1=在售 2=待审核 */
    private Integer status;
    private Integer salesCount;
    private BigDecimal price;       // 默认价格（展示用）
    private Integer stock;          // 总库存冗余
    private LocalDateTime createTime;

    // 联表/组装字段（非数据库列，查询时填充）
    private List<ItemSku> skus;
    private List<String> imageUrls;
}
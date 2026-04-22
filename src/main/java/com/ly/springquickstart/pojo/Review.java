package com.ly.springquickstart.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品评价
 * 对应数据库表 item_reviews（建表 SQL 见 ReviewMapper 注释）
 */
@Data
public class Review {
    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long itemId;
    /** 评分 1-5 */
    private Integer score;
    private String content;
    private String images;
    private LocalDateTime createTime;

    // 非数据库列
    private String username;
    private String itemTitle;
}

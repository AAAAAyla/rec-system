package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Review;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 商品评价 Mapper
 *
 * 建表 SQL（首次使用前执行）：
 * CREATE TABLE IF NOT EXISTS item_reviews (
 *   id            BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   order_id      BIGINT NOT NULL,
 *   order_item_id BIGINT NOT NULL,
 *   user_id       BIGINT NOT NULL,
 *   item_id       BIGINT NOT NULL,
 *   score         TINYINT NOT NULL DEFAULT 5 COMMENT '1-5分',
 *   content       TEXT,
 *   images        VARCHAR(1000) COMMENT '图片URL，英文逗号分隔',
 *   create_time   DATETIME DEFAULT NOW(),
 *   UNIQUE KEY uk_order_item (order_item_id)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 */
@Mapper
public interface ReviewMapper {

    @Insert("""
        INSERT INTO item_reviews(order_id, order_item_id, user_id, item_id, score, content, images)
        VALUES(#{orderId}, #{orderItemId}, #{userId}, #{itemId}, #{score}, #{content}, #{images})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Review review);

    /** 查询某商品的评价列表（分页） */
    @Select("""
        SELECT r.*, u.username, i.title AS item_title
        FROM item_reviews r
        LEFT JOIN users u ON u.id = r.user_id
        LEFT JOIN items i ON i.id = r.item_id
        WHERE r.item_id = #{itemId}
        ORDER BY r.create_time DESC
        LIMIT #{offset}, #{size}
        """)
    List<Review> findByItem(@Param("itemId") Long itemId,
                            @Param("offset") int offset,
                            @Param("size") int size);

    @Select("SELECT COUNT(*) FROM item_reviews WHERE item_id = #{itemId}")
    int countByItem(Long itemId);

    /** 查询某用户的评价列表 */
    @Select("SELECT * FROM item_reviews WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Review> findByUser(Long userId);

    /** 检查某订单明细是否已评价 */
    @Select("SELECT COUNT(*) FROM item_reviews WHERE order_item_id = #{orderItemId}")
    int existsByOrderItem(Long orderItemId);

    /** 商品平均评分 */
    @Select("SELECT IFNULL(AVG(score), 0) FROM item_reviews WHERE item_id = #{itemId}")
    Double avgScore(Long itemId);
}

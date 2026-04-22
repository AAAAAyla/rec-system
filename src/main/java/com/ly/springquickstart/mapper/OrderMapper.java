package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Order;
import com.ly.springquickstart.pojo.OrderItem;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderMapper {

    // ── 订单主表 ──────────────────────────────────────

    @Insert("""
        INSERT INTO orders(order_no, user_id, merchant_id, status,
            total_amount, freight_amount, pay_amount, address_snapshot, remark)
        VALUES(#{orderNo}, #{userId}, #{merchantId}, #{status},
            #{totalAmount}, #{freightAmount}, #{payAmount},
            #{addressSnapshot, typeHandler=com.ly.springquickstart.handler.JsonTypeHandler},
            #{remark})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);

    @Select("SELECT * FROM orders WHERE id=#{id}")
    Order findById(Long id);

    @Select("SELECT * FROM orders WHERE order_no=#{orderNo}")
    Order findByOrderNo(String orderNo);

    // 使用 <script> 标签包裹，是 MyBatis 注解模式下动态 SQL 的正确写法
    @Select("""
        <script>
        SELECT * FROM orders WHERE user_id=#{userId}
        <if test='status != null'> AND status=#{status} </if>
        ORDER BY create_time DESC LIMIT #{offset}, #{size}
        </script>
        """)
    List<Order> findByUser(@Param("userId") Long userId,
                           @Param("status") Integer status,
                           @Param("offset") int offset,
                           @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM orders WHERE user_id=#{userId}
        <if test='status != null'> AND status=#{status} </if>
        </script>
        """)
    int countByUser(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("""
        <script>
        SELECT * FROM orders WHERE merchant_id=#{merchantId}
        <if test='status != null'> AND status=#{status} </if>
        ORDER BY create_time DESC LIMIT #{offset}, #{size}
        </script>
        """)
    List<Order> findByMerchant(@Param("merchantId") Long merchantId,
                               @Param("status") Integer status,
                               @Param("offset") int offset,
                               @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM orders WHERE merchant_id=#{merchantId}
        <if test='status != null'> AND status=#{status} </if>
        </script>
        """)
    int countByMerchant(@Param("merchantId") Long merchantId, @Param("status") Integer status);

    /** 查询超时未付款订单 */
    @Select("SELECT * FROM orders WHERE status=0 AND create_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    List<Order> findExpiredUnpaid(@Param("minutes") long minutes);

    /** 安全更新：带旧状态校验，防止并发错误流转 */
    @Update("""
        UPDATE orders SET status=#{newStatus}, update_time=NOW()
        WHERE id=#{id} AND status=#{oldStatus}
        """)
    int compareAndSetStatus(@Param("id") Long id,
                            @Param("oldStatus") int oldStatus,
                            @Param("newStatus") int newStatus);

    @Update("UPDATE orders SET status=#{status}, pay_time=NOW() WHERE id=#{id} AND status=0")
    int markPaid(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE orders SET status=4, cancel_reason=#{reason} WHERE id=#{id} AND status=#{allowedStatus}")
    int cancel(@Param("id") Long id,
               @Param("allowedStatus") int allowedStatus,
               @Param("reason") String reason);

    // ── 订单明细 ──────────────────────────────────────

    @Insert("""
        INSERT INTO order_items(order_id, item_id, sku_id, title, image_url, spec_json, price, quantity)
        VALUES(#{orderId}, #{itemId}, #{skuId}, #{title}, #{imageUrl}, #{specJson}, #{price}, #{quantity})
        """)
    void insertItem(OrderItem item);

    @Select("SELECT * FROM order_items WHERE order_id=#{orderId}")
    List<OrderItem> findItemsByOrderId(Long orderId);
}

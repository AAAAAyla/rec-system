package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.OrderMapper;
import com.ly.springquickstart.mapper.ReviewMapper;
import com.ly.springquickstart.pojo.Order;
import com.ly.springquickstart.pojo.OrderItem;
import com.ly.springquickstart.pojo.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired private ReviewMapper reviewMapper;
    @Autowired private OrderMapper  orderMapper;

    /**
     * 提交评价
     * @param userId    买家 ID
     * @param orderId   订单 ID
     * @param reviews   评价列表，每条对应一个订单明细
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitReviews(Long userId, Long orderId, List<Map<String, Object>> reviews) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getUserId().equals(userId))
            throw new RuntimeException("订单不存在");
        if (order.getStatus() != 3)
            throw new RuntimeException("只有已完成订单才能评价");

        List<OrderItem> items = orderMapper.findItemsByOrderId(orderId);

        for (Map<String, Object> rev : reviews) {
            Long orderItemId = toLong(rev.get("orderItemId"));
            Integer score    = toInt(rev.get("score"), 5);
            String content   = (String) rev.getOrDefault("content", "");
            String images    = (String) rev.getOrDefault("images", "");

            // 校验该订单明细属于本订单
            OrderItem orderItem = items.stream()
                    .filter(i -> i.getId() != null && i.getId().equals(orderItemId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("订单明细不存在：" + orderItemId));

            if (reviewMapper.existsByOrderItem(orderItemId) > 0)
                throw new RuntimeException("已评价过该商品：" + orderItem.getTitle());

            if (score < 1 || score > 5)
                throw new RuntimeException("评分必须在 1~5 之间");

            Review review = new Review();
            review.setOrderId(orderId);
            review.setOrderItemId(orderItemId);
            review.setUserId(userId);
            review.setItemId(orderItem.getItemId());
            review.setScore(score);
            review.setContent(content);
            review.setImages(images);
            reviewMapper.insert(review);
        }
    }

    /** 查询商品评价列表（分页） */
    public Map<String, Object> listByItem(Long itemId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Review> rows  = reviewMapper.findByItem(itemId, offset, pageSize);
        int total          = reviewMapper.countByItem(itemId);
        Double avgScore    = reviewMapper.avgScore(itemId);
        return Map.of("rows", rows, "total", total, "avgScore", avgScore);
    }

    /** 查询当前用户的所有评价 */
    public List<Review> myReviews(Long userId) {
        return reviewMapper.findByUser(userId);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        return Long.parseLong(val.toString());
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        return Integer.parseInt(val.toString());
    }
}

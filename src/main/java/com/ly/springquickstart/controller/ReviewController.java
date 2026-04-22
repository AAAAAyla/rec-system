package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.ReviewService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品评价接口
 */
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 提交订单评价（批量）
     * body: {
     *   "orderId": 1,
     *   "reviews": [
     *     { "orderItemId": 10, "score": 5, "content": "很好！", "images": "url1,url2" }
     *   ]
     * }
     */
    @PostMapping
    public Result submit(@RequestBody Map<String, Object> body) {
        Long orderId = Long.parseLong(body.get("orderId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reviews = (List<Map<String, Object>>) body.get("reviews");
        reviewService.submitReviews(uid(), orderId, reviews);
        return Result.success("评价成功");
    }

    /**
     * 查询商品评价列表（公开）
     * GET /reviews/items/{itemId}?pageNum=1&pageSize=10
     */
    @GetMapping("/items/{itemId}")
    public Result listByItem(@PathVariable Long itemId,
                             @RequestParam(defaultValue = "1")  int pageNum,
                             @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reviewService.listByItem(itemId, pageNum, pageSize));
    }

    /** 查询我的所有评价 */
    @GetMapping("/me")
    public Result myReviews() {
        return Result.success(reviewService.myReviews(uid()));
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }
}

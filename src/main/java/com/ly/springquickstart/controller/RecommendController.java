package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("/{userId}")
    public Result getRecommend(@PathVariable Integer userId) {
        List<Integer> list = recommendService.getRecommendList(userId);
        return Result.success(list);
    }

    @PostMapping("/mock-algorithm")
    public Result mockAlgorithmData(Integer userId, String itemIds) {
        recommendService.saveRecommendData(userId, itemIds);
        return Result.success("数据写入 Redis 成功");
    }

}
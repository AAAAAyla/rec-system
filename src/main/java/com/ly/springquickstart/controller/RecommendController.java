package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.RecommendService;
import com.ly.springquickstart.utils.ThreadLocalUtil; // 引入储物柜工具
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 改造后的新接口：获取当前登录用户的推荐列表
     * 注意：路径变成了 "/me"，不再接收前端传来的 userId 参数
     */
    @GetMapping("/me")
    public Result getMyRecommend() {
        // 1. 从“专属储物柜”里拿出当前登录用户的信息
        // （能走到这里，说明保安已经验过票了，储物柜里一定有这个人的信息）
        Map<String, Object> claims = ThreadLocalUtil.get();

        // 2. 从字典(Map)中提取出真正的 userId
        // 这里必须强转为 Integer，键名 "id" 要和生成 JWT 时候放进去的键名完全一致
        Integer currentUserId = (Integer) claims.get("id");

        // 3. 用这个绝对安全的、从手环里拆出来的 currentUserId 去查数据
        List<Integer> list = recommendService.getRecommendList(currentUserId);

        return Result.success(list);
    }

    /**
     * 模拟算法服务写入推荐结果的接口
     */
    @PostMapping("/mock-algorithm")
    public Result writeMockData(@RequestParam Integer userId, @RequestBody List<Integer> itemIds) {
        // 1. 把前端传来的 List (例如 [999, 888, 777]) 转换成普通的字符串 "[999, 888, 777]"
        String idsString = itemIds.toString();

        // 2. 名字和参数类型现在都完美匹配了你的 Service！
        recommendService.saveRecommendData(userId, idsString);

        return Result.success(null);
    }
}
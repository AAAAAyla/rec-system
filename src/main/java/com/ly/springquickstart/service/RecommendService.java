package com.ly.springquickstart.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RecommendService {

    // Spring Boot 自动装配的 Redis 操作工具类
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public List<Integer> getRecommendList(Integer userId) {
        // 1. 拼接 Redis 的 Key，规范的格式通常是 "业务名:实体名:id"
        String key = "recommend:user:" + userId;

        // 2. 去 Redis 里查数据
        String cachedData = stringRedisTemplate.opsForValue().get(key);

        // 3. 判断是否命中缓存
        if (cachedData != null) {
            System.out.println("====== 命中 Redis 缓存！用户ID: " + userId + " ======");
            // 假设 Redis 里存的是 "101,202,303" 这样的字符串，把它切分成 List
            return Arrays.stream(cachedData.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } else {
            System.out.println("====== 未命中缓存，走兜底推荐逻辑！用户ID: " + userId + " ======");
            // 如果 Redis 里没有，就返回一个默认的热门商品列表
            return Arrays.asList(101, 102, 103, 104);
        }
    }
    public void saveRecommendData(Integer userId, String itemIds) {
        String key = "recommend:user:" + userId;
        // 将字符串写入 Redis，并设置 24 小时的过期时间
        stringRedisTemplate.opsForValue().set(key, itemIds, 24, TimeUnit.HOURS);
        System.out.println("====== 成功将算法推荐结果写入 Redis！用户ID: " + userId + " ======");
    }
}
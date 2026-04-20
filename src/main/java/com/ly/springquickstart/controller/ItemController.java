package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.utils.ThreadLocalUtil; // 👈 必须引入储物柜工具！
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 原来的普通分页查询接口 (保留)
    @GetMapping("/page")
    public Result getItems(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM items WHERE image_url IS NOT NULL LIMIT ? OFFSET ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, pageSize, offset);
        String countSql = "SELECT COUNT(*) FROM items WHERE image_url IS NOT NULL";
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("total", total);
        return Result.success(data);
    }

    // 修改后的个性化推荐接口：彻底告别伪造参数！
    @GetMapping("/recommend")
    public Result getRecommend() { // 👈 注意：括号里空了！不需要前端传 userId 了

        // 1. 从储物柜拿信息
        Map<String, Object> claims = ThreadLocalUtil.get();

        //  强壮写法：不管底层存的是 Integer 还是 Long，统统先转成 String 再解析，绝不翻车！
        Integer userId = Integer.parseInt(claims.get("id").toString());


        // 2. 去 Redis 传菜窗口找这个用户的专属菜单
        String redisKey = "rec:user:" + userId;
        String recJson = stringRedisTemplate.opsForValue().get(redisKey);

        List<Map<String, Object>> list;

        // 3. 如果 Redis 里有数据
        if (recJson != null && !recJson.isEmpty() && !recJson.equals("[]")) {
            // 把中括号去掉，变成 "101, 205, 308"
            String idsStr = recJson.replace("[", "").replace("]", "");

            // 去 MySQL 查出这 10 个商品的图文信息！
            String sql = "SELECT * FROM items WHERE id IN (" + idsStr + ")";
            list = jdbcTemplate.queryForList(sql);
        } else {
            // 4. 兜底方案：如果 Redis 没数据，随便给 10 个有图片的商品
            String fallbackSql = "SELECT * FROM items WHERE image_url IS NOT NULL LIMIT 10";
            list = jdbcTemplate.queryForList(fallbackSql);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("total", list.size());

        return Result.success(data);
    }
}
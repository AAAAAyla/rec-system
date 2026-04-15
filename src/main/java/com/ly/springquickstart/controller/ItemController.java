package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
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
    private JdbcTemplate jdbcTemplate; // 简单起见，我们直接用 JdbcTemplate 查询

    @GetMapping("/page")
    public Result getItems(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        // 查询带有标题和图片的商品
        String sql = "SELECT * FROM items WHERE image_url IS NOT NULL LIMIT ? OFFSET ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, pageSize, offset);

        // 查询总数用于分页
        String countSql = "SELECT COUNT(*) FROM items WHERE image_url IS NOT NULL";
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);

        Map<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("total", total);

        return Result.success(data);
    }
}
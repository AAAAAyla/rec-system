package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Rating;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @GetMapping
    public Result list() {
        return Result.success(ratingService.findAll());
    }

    @GetMapping("/user/{userId}")
    public Result byUser(@PathVariable Integer userId) {
        return Result.success(ratingService.findByUserId(userId));
    }

    @GetMapping("/stats")
    public Result stats() {
        return Result.success(ratingService.getStats());
    }

    @PostMapping
    public Result add(@RequestBody Rating rating) {
        ratingService.add(rating);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        ratingService.delete(id);
        return Result.success();
    }

    @GetMapping("/page")
    public Result page(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "5") Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<Rating> rows = ratingService.findByPage(offset, pageSize);
        Integer total = ratingService.count();

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        return Result.success(result);
    }
}
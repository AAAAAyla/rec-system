package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.RatingMapper;
import com.ly.springquickstart.pojo.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class RatingService {

    @Autowired
    private RatingMapper ratingMapper;

    public List<Rating> findAll() {
        return ratingMapper.findAll();
    }

    public List<Rating> findByUserId(Integer userId) {
        return ratingMapper.findByUserId(userId);
    }

    public List<Map<String, Object>> getStats() {
        return ratingMapper.getStats();
    }

    public void add(Rating rating) {
        ratingMapper.insert(rating);
    }

    public void delete(Integer id) {
        ratingMapper.deleteById(id);
    }

    /**
     * 获取分页数据
     * @param offset 跳过多少条
     * @param pageSize 拿多少条
     * @return 这一页的数据列表
     */
    public List<Rating> findByPage(Integer offset, Integer pageSize) {
        // Service 就像个跑腿的，直接调用 Mapper 去查数据库，然后把结果返回给 Controller
        return ratingMapper.findByPage(offset, pageSize);
    }

    /**
     * 获取数据总条数
     * @return 数据库里 rating 表的总行数
     */
    public Integer count() {
        return ratingMapper.count();
    }
}
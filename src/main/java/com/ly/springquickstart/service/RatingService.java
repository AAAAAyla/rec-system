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
}
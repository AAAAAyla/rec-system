package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Rating;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RatingMapper {

    @Select("SELECT * FROM rating")
    List<Rating> findAll();

    @Select("SELECT * FROM rating WHERE user_id = #{userId}")
    List<Rating> findByUserId(Integer userId);

    @Select("SELECT domain, COUNT(*) as count, AVG(score) as avgScore FROM rating GROUP BY domain")
    List<java.util.Map<String, Object>> getStats();

    @Insert("INSERT INTO rating(user_id, item_id, score, domain) VALUES(#{userId}, #{itemId}, #{score}, #{domain})")
    void insert(Rating rating);

    @Delete("DELETE FROM rating WHERE id = #{id}")
    void deleteById(Integer id);

    @Select("SELECT * FROM rating LIMIT #{offset}, #{size}")
    List<Rating> findByPage(@Param("offset") Integer offset, @Param("size") Integer size);

    @Select("SELECT COUNT(*) FROM rating")
    Integer count();
}
package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("SELECT * FROM categories WHERE status = 1 ORDER BY sort")
    List<Category> findAll();

    @Select("SELECT * FROM categories WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort")
    List<Category> findByParent(int parentId);

    @Select("SELECT * FROM categories WHERE id = #{id}")
    Category findById(int id);
}
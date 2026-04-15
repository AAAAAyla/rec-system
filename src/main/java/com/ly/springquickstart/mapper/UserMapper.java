package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper  // 告诉Spring Boot：这是一个翻译官
public interface UserMapper {

    // 查询所有用户
    // 翻译成SQL：SELECT * FROM user
    @Select("SELECT * FROM user")
    List<User> findAll();

    // 根据ID查询用户
    // 翻译成SQL：SELECT * FROM user WHERE id = ?
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);

    // 新增用户
    // 翻译成SQL：INSERT INTO user(username, password, email) VALUES(?, ?, ?)
    @Insert("INSERT INTO user(username, password, email) VALUES(#{username}, #{password}, #{email})")
    void insert(User user);

    // 修改用户
    @Update("UPDATE user SET username=#{username}, email=#{email} WHERE id=#{id}")
    void update(User user);

    // 删除用户
    @Delete("DELETE FROM user WHERE id = #{id}")
    void deleteById(Integer id);
}
package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper  // 告诉Spring Boot：这是一个翻译官
public interface UserMapper {

    // 查询所有用户
    // 翻译成SQL：SELECT * FROM user
    @Select("SELECT * FROM users")
    List<User> findAll();

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Integer id);

    @Insert("INSERT INTO users(username, password, email) VALUES(#{username}, #{password}, #{email})")
    void insert(User user);

    @Update("UPDATE users SET username=#{username}, email=#{email} WHERE id=#{id}")
    void update(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(Integer id);
}
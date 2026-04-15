package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.UserMapper;
import com.ly.springquickstart.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service  // 告诉Spring Boot：这是业务逻辑层
public class UserService {

    @Autowired  // 自动注入翻译官
    private UserMapper userMapper;

    public List<User> findAll() {
        return userMapper.findAll();
    }

    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    public void add(User user) {
        userMapper.insert(user);
    }

    public void update(User user) {
        userMapper.update(user);
    }

    public void delete(Integer id) {
        userMapper.deleteById(id);
    }
}
package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.pojo.User;
import com.ly.springquickstart.service.UserService;
import com.ly.springquickstart.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController  // 接待员
@RequestMapping("/users")  // 所有接口以 /users 开头
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public Result list() {
        return Result.success(userService.findAll());
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer id) {
        return Result.success(userService.findById(id));
    }

    @PostMapping
    public Result add(@RequestBody User user) {
        userService.add(user);
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody User user) {
        userService.update(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        userService.delete(id);
        return Result.success();
    }

    @PostMapping("/login")
    public Result login(String username, String password) {
        // 1. 验证账号密码（正式项目这里会去数据库查）
        if ("test".equals(username) && "123".equals(password)) {
            // 2. 账号密码正确，准备给用户发手环！
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", 1);
            claims.put("username", username);

            // 3. 调用机器生成手环
            String token = JwtUtils.genToken(claims);

            // 4. 把手环给前端
            return Result.success(token);
        }

        return Result.error("用户名或密码错误");
    }


}
package com.ly.springquickstart.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data  // 自动生成get/set/toString，不用手写
public class User {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private LocalDateTime createTime;
}
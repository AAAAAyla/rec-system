package com.ly.springquickstart.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Rating {
    private Integer id;
    private Integer userId;
    private Integer itemId;
    private Double score;
    private String domain;
    private LocalDateTime createTime;
}
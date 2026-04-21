// src/main/java/com/ly/springquickstart/pojo/Category.java
package com.ly.springquickstart.pojo;

import lombok.Data;
import java.util.List;

@Data
public class Category {
    private Integer id;
    private Integer parentId;
    private String name;
    private Integer level;
    private Integer sort;
    private String iconUrl;
    private Integer status;

    // 非数据库列，CategoryService 里手动组装
    private List<Category> children;
}
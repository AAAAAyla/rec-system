package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.CategoryMapper;
import com.ly.springquickstart.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 返回三级分类树
     */
    public List<Category> tree() {
        List<Category> all = categoryMapper.findAll();

        // 按 parentId 分组
        Map<Integer, List<Category>> grouped = all.stream()
                .collect(Collectors.groupingBy(Category::getParentId));

        // 组装树形
        List<Category> roots = grouped.getOrDefault(0, new ArrayList<>());
        for (Category root : roots) {
            List<Category> level2 = grouped.getOrDefault(root.getId(), new ArrayList<>());
            for (Category l2 : level2) {
                l2.setChildren(grouped.getOrDefault(l2.getId(), new ArrayList<>()));
            }
            root.setChildren(level2);
        }
        return roots;
    }
}

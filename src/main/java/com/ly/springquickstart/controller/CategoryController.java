package com.ly.springquickstart.controller;

import com.ly.springquickstart.annotation.RoleRequired;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.CategoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired private CategoryService categoryService;
    @Autowired private JdbcTemplate jdbcTemplate;
    private final ChatClient chatClient;

    public CategoryController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/tree")
    public Result tree() {
        return Result.success(categoryService.tree());
    }

    /**
     * AI 自动识别分类：传入商品标题，返回推荐的 categoryId
     */
    @PostMapping("/auto-classify")
    public Result autoClassify(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) return Result.error("标题不能为空");

        List<Map<String, Object>> cats = jdbcTemplate.queryForList(
                "SELECT id, name, parent_id FROM categories ORDER BY parent_id, id");
        if (cats.isEmpty()) return Result.error("暂无分类数据");

        String catList = cats.stream()
                .map(c -> "id=" + c.get("id") + ",名称=" + c.get("name") + ",parent_id=" + c.get("parent_id"))
                .collect(Collectors.joining("\n"));

        try {
            String aiReply = chatClient.prompt(new Prompt(List.of(
                    new SystemMessage("""
                        你是一个商品分类助手。用户给你一个商品标题，你需要从下方的分类列表中选出最匹配的一个分类ID。
                        只回复一个数字(分类ID)，不要回复任何其他内容。
                        优先选择二级分类(parent_id!=0)；如果没有合适的二级分类，则选一级分类。
                        
                        分类列表：
                        """ + catList),
                    new UserMessage("商品标题：" + title)
            ))).call().content();

            String cleaned = aiReply.replaceAll("[^0-9]", "");
            if (!cleaned.isEmpty()) {
                int catId = Integer.parseInt(cleaned);
                return Result.success(Map.of("categoryId", catId));
            }
            return Result.error("无法识别分类");
        } catch (Exception e) {
            return Result.error("AI 分类识别失败：" + e.getMessage());
        }
    }

    @GetMapping("/all")
    @RoleRequired({2})
    public Result allFlat() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM categories ORDER BY parent_id, sort");
        return Result.success(rows);
    }

    @PostMapping
    @RoleRequired({2})
    public Result create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        int parentId = body.get("parentId") != null ? Integer.parseInt(body.get("parentId").toString()) : 0;
        int level = parentId == 0 ? 1 : 2;
        String iconUrl = (String) body.getOrDefault("iconUrl", "");
        int sort = body.get("sort") != null ? Integer.parseInt(body.get("sort").toString()) : 0;

        jdbcTemplate.update(
                "INSERT INTO categories (parent_id, name, level, sort, icon_url) VALUES (?, ?, ?, ?, ?)",
                parentId, name, level, sort, iconUrl);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RoleRequired({2})
    public Result update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String iconUrl = (String) body.getOrDefault("iconUrl", "");
        int sort = body.get("sort") != null ? Integer.parseInt(body.get("sort").toString()) : 0;

        jdbcTemplate.update("UPDATE categories SET name=?, icon_url=?, sort=? WHERE id=?",
                name, iconUrl, sort, id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RoleRequired({2})
    public Result delete(@PathVariable Integer id) {
        Integer childCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE parent_id=?", Integer.class, id);
        if (childCount != null && childCount > 0) {
            return Result.error("该分类下有子分类，请先删除子分类");
        }
        jdbcTemplate.update("DELETE FROM categories WHERE id=?", id);
        return Result.success();
    }
}

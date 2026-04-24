package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired private RagService ragService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Pattern PRODUCT_LINK = Pattern.compile("\\[([^\\]]+)]\\(product://(\\d+)\\)");

    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) request.get("history");

        @SuppressWarnings("unchecked")
        Map<String, Object> pageContext = (Map<String, Object>) request.get("pageContext");

        String extraContext = buildPageContextString(pageContext);

        try {
            String aiReply = ragService.chatWithRag(userMessage, history, extraContext);

            List<Map<String, Object>> products = extractProducts(aiReply);
            String cleanText = PRODUCT_LINK.matcher(aiReply).replaceAll("$1");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("text", cleanText);
            data.put("products", products);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("text", "抱歉，AI 助手暂时出了点问题，请稍后再试。");
            data.put("products", List.of());
            return Result.success(data);
        }
    }

    private String buildPageContextString(Map<String, Object> ctx) {
        if (ctx == null || ctx.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String page = (String) ctx.getOrDefault("page", "");

        switch (page) {
            case "product" -> {
                sb.append("用户正在浏览商品详情页。");
                if (ctx.get("productName") != null) sb.append(" 商品名：").append(ctx.get("productName")).append("。");
                if (ctx.get("productPrice") != null) sb.append(" 价格：").append(ctx.get("productPrice")).append("。");
                if (ctx.get("productId") != null) {
                    try {
                        Long pid = Long.parseLong(ctx.get("productId").toString());
                        List<Map<String, Object>> info = jdbcTemplate.queryForList(
                                "SELECT id, title, price FROM items WHERE id = ? AND status = 1", pid);
                        if (!info.isEmpty()) {
                            Map<String, Object> item = info.get(0);
                            sb.append(" 数据库信息：").append(item.get("title")).append("，￥").append(item.get("price")).append("。");
                        }
                    } catch (Exception ignored) {}
                }
            }
            case "cart" -> sb.append("用户正在查看购物车。");
            case "search" -> {
                sb.append("用户正在搜索商品。");
                if (ctx.get("searchKeyword") != null) sb.append(" 搜索关键词：").append(ctx.get("searchKeyword")).append("。");
            }
            case "orders" -> sb.append("用户正在查看订单列表。");
            case "checkout" -> sb.append("用户正在结算下单。");
            case "favorites" -> sb.append("用户正在查看收藏夹。");
            case "merchant" -> sb.append("用户是商家，正在使用商家管理后台。");
            case "admin" -> sb.append("用户是管理员，正在使用管理后台。");
            case "home" -> sb.append("用户在首页浏览。");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> extractProducts(String aiReply) {
        Matcher matcher = PRODUCT_LINK.matcher(aiReply);
        List<Long> ids = new ArrayList<>();
        while (matcher.find()) {
            try { ids.add(Long.parseLong(matcher.group(2))); } catch (NumberFormatException ignored) {}
        }
        if (ids.isEmpty()) return List.of();

        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        String sql = "SELECT id, title, price, image_url FROM items WHERE id IN (" + placeholders + ") AND status = 1";
        return jdbcTemplate.queryForList(sql, ids.toArray());
    }
}

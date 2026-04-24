package com.ly.springquickstart.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    private volatile boolean ready = false;

    public RagService(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = new SimpleVectorStore(embeddingModel);
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initKnowledgeBase() {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[RAG] 后台开始构建商品向量知识库...");
                String sql = "SELECT id, title, price, image_url FROM items WHERE status = 1 LIMIT 200";
                List<Map<String, Object>> items = jdbcTemplate.queryForList(sql);

                List<Document> documents = items.stream().map(item -> {
                    String title = (String) item.get("title");
                    Object price = item.get("price");
                    String content = "商品ID: " + item.get("id") + " | 名称: " + title + " | 价格: ￥" + price;
                    Map<String, Object> metadata = Map.of(
                            "id", item.get("id"),
                            "title", title,
                            "price", price != null ? price.toString() : "0"
                    );
                    return new Document(content, metadata);
                }).collect(Collectors.toList());

                if (!documents.isEmpty()) {
                    vectorStore.add(documents);
                    System.out.println("[RAG] 向量知识库构建完成，共 " + documents.size() + " 个商品");
                }
                ready = true;
            } catch (Exception e) {
                System.err.println("[RAG] 向量库构建失败（不影响其他功能）: " + e.getMessage());
                ready = true;
            }
        });
    }

    // ── 意图识别 ─────────────────────────────────────

    private enum Intent { GENERAL_RECOMMEND, SPECIFIC_SEARCH, FOLLOW_UP, COUPON_QUERY }

    private Intent detectIntent(String msg) {
        if (msg.length() <= 30) {
            String[] followUpWords = {
                    "它", "这个", "那个", "这本", "那本", "这款", "那款",
                    "是什么", "什么类型", "什么类别", "什么种类", "属于",
                    "怎么样", "好不好", "值不值", "划算吗", "贵吗", "便宜吗",
                    "多少钱", "价格", "评价", "评分",
                    "为什么", "怎么用", "适合", "区别", "对比",
                    "介绍一下", "详细", "具体", "说说", "讲讲"
            };
            for (String kw : followUpWords) {
                if (msg.contains(kw)) return Intent.FOLLOW_UP;
            }
        }

        String[] couponWords = {"优惠券", "优惠", "打折", "折扣", "满减", "券"};
        for (String kw : couponWords) {
            if (msg.contains(kw)) return Intent.COUPON_QUERY;
        }

        String[] generalWords = {
                "推荐", "热销", "热门", "畅销", "新品", "上架",
                "随便看看", "逛逛", "有什么好", "什么值得买",
                "性价比", "便宜", "划算",
                "送礼", "礼物"
        };
        for (String kw : generalWords) {
            if (msg.contains(kw)) return Intent.GENERAL_RECOMMEND;
        }

        return Intent.SPECIFIC_SEARCH;
    }

    // ── 通用推荐：直接从数据库取商品 ───────────────────

    private List<Map<String, Object>> fetchRecommendItems(int limit) {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT id, title, price, image_url FROM items WHERE status = 1 ORDER BY RAND() LIMIT ?", limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> fetchAvailableCoupons() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT title, discount, min_amount, expire_time FROM coupons WHERE remain_count > 0 AND expire_time > NOW() LIMIT 10");
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> fetchCheapItems(int limit) {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT id, title, price, image_url FROM items WHERE status = 1 AND price IS NOT NULL ORDER BY price ASC LIMIT ?", limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    // ── 关键词搜索 ──────────────────────────────────

    private List<Map<String, Object>> searchItemsByKeywords(String userMessage) {
        String[] stopWords = {"帮我", "帮忙", "找", "找找", "搜", "搜索", "一", "个", "篇", "本",
                "推荐", "有没有", "想要", "想买", "什么", "好的", "最好", "的", "吗",
                "呢", "啊", "了", "我", "开头", "相关", "类似", "关于", "请", "能不能",
                "可以", "有", "看看", "给我", "来", "几"};
        String cleaned = userMessage;
        for (String sw : stopWords) {
            cleaned = cleaned.replace(sw, " ");
        }

        List<String> keywords = new ArrayList<>();
        for (String part : cleaned.trim().split("\\s+")) {
            String w = part.trim().replaceAll("[？?！!。，,.\\s]", "");
            if (w.length() >= 2 || (w.length() == 1 && w.matches("[A-Za-z]"))) {
                keywords.add(w);
            }
        }

        if (keywords.isEmpty()) return List.of();

        StringBuilder sql = new StringBuilder("SELECT id, title, price, image_url FROM items WHERE status = 1 AND (");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("title LIKE ?");
            params.add("%" + keywords.get(i) + "%");
        }
        sql.append(") LIMIT 10");

        try {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            return List.of();
        }
    }

    // ── 格式化商品列表为文本 ─────────────────────────

    private String formatItems(List<Map<String, Object>> items) {
        return items.stream()
                .map(item -> "商品ID: " + item.get("id") + " | 名称: " + item.get("title") + " | 价格: ￥" + item.get("price"))
                .collect(Collectors.joining("\n"));
    }

    // ── 主对话方法 ──────────────────────────────────

    public String chatWithRag(String userMessage, List<Map<String, String>> history, String pageContext) {
        if (!ready) {
            return "AI 助手正在初始化知识库，请稍等片刻...";
        }

        Intent intent = detectIntent(userMessage);
        String combinedContext;
        String intentHint;

        switch (intent) {
            case COUPON_QUERY -> {
                List<Map<String, Object>> coupons = fetchAvailableCoupons();
                if (coupons.isEmpty()) {
                    combinedContext = "【优惠券查询结果】：当前没有任何可用优惠券。";
                    intentHint = "用户询问优惠券/打折信息。数据库查询结果是：当前没有可用优惠券。你必须如实告知用户[目前没有可用的优惠券]，不要编造或推荐不存在的优惠活动。可以推荐一些高性价比商品作为替代。";
                } else {
                    StringBuilder sb = new StringBuilder("【当前可用优惠券】：\n");
                    for (Map<String, Object> c : coupons) {
                        sb.append("- ").append(c.get("title"))
                          .append("（满").append(c.get("min_amount")).append("减").append(c.get("discount")).append("）\n");
                    }
                    combinedContext = sb.toString();
                    intentHint = "用户询问优惠券。以下是真实的优惠券数据，请如实介绍。不要编造不存在的券。";
                }
            }
            case FOLLOW_UP -> {
                combinedContext = "";
                intentHint = "用户在追问上一轮的内容，根据对话历史回答即可，不需要重新搜索。";
            }
            case GENERAL_RECOMMEND -> {
                boolean wantCheap = userMessage.contains("性价比") || userMessage.contains("便宜")
                        || userMessage.contains("划算") || userMessage.contains("优惠");
                List<Map<String, Object>> items = wantCheap ? fetchCheapItems(8) : fetchRecommendItems(8);
                combinedContext = formatItems(items);
                intentHint = "用户想要通用推荐，从下面的商品中挑 3~5 个推荐给用户。必须推荐，不要反问！";
            }
            case SPECIFIC_SEARCH -> {
                // 关键词搜索
                List<Map<String, Object>> dbResults = searchItemsByKeywords(userMessage);

                // 向量语义搜索
                String vectorContext = "";
                try {
                    List<Document> docs = vectorStore.similaritySearch(userMessage);
                    vectorContext = docs.stream().map(Document::getContent).collect(Collectors.joining("\n"));
                } catch (Exception e) {
                    System.err.println("[RAG] 向量检索异常: " + e.getMessage());
                }

                String dbContext = formatItems(dbResults);

                if (!dbContext.isEmpty()) {
                    combinedContext = dbContext + (vectorContext.isEmpty() ? "" : "\n" + vectorContext);
                } else if (!vectorContext.isEmpty()) {
                    combinedContext = vectorContext;
                } else {
                    // 兜底：如果啥都搜不到，给一些随机商品让 AI 至少能推荐点东西
                    List<Map<String, Object>> fallback = fetchRecommendItems(5);
                    combinedContext = fallback.isEmpty() ? "（数据库中暂无商品）" : formatItems(fallback);
                }
                intentHint = "用户在搜索特定商品，从商品数据中找最匹配的推荐。如果没有精确匹配，推荐最接近的。";
            }
            default -> {
                combinedContext = "";
                intentHint = "";
            }
        }

        String pageCtxBlock = (pageContext != null && !pageContext.isEmpty())
                ? "\n【用户场景】：" + pageContext
                : "";

        String systemPrompt = """
                你是 AmazonRec 商城的 AI 购物助手"小A"。
                
                当前任务提示：""" + intentHint + """
                
                
                ⚠️ 绝对规则（违反即失败）：
                1. 当商品数据不为空时，你必须从中挑选商品推荐，使用格式：[商品名称](product://商品ID)
                2. 绝对不要回复"请问您需要什么？"这种反问，直接推荐！
                3. 推荐 3~5 个商品，每个用 [名称](product://ID) 格式
                4. 追问时根据历史回答，不要说"找不到"
                5. 回复不超过 150 字，简洁直接
                6. 绝对诚实：如果数据显示没有优惠券/折扣，你必须说"目前没有"，绝不能编造不存在的优惠
                7. 不要推荐你不确定是否存在的东西（优惠、折扣、活动等）
                
                示例回复（推荐场景）：
                "给你推荐几本好书：
                [The Prophet](product://42)
                [erta ale](product://15)
                [Norwegian Wood](product://8)
                都很值得一看！"
                
                """ + (combinedContext.isEmpty() ? "" : "【商品数据】：\n" + combinedContext) + pageCtxBlock;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        if (history != null) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("ai".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            }
        }

        messages.add(new UserMessage(userMessage));

        Prompt prompt = new Prompt(messages);
        return chatClient.prompt(prompt).call().content();
    }
}

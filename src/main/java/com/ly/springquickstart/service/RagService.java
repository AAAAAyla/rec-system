package com.ly.springquickstart.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /** true = 向量库已就绪，false = 仍在后台构建中 */
    private volatile boolean ready = false;

    @Autowired
    public RagService(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = new SimpleVectorStore(embeddingModel);
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 异步初始化：在后台线程构建向量库，不阻塞 Spring Boot 启动
     * 最多只索引最新 200 条商品，避免 API 调用过多
     */
    @PostConstruct
    public void initKnowledgeBase() {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[RAG] 后台开始构建商品向量知识库...");
                String sql = "SELECT id, title FROM items WHERE status = 1 LIMIT 200";
                List<Map<String, Object>> items = jdbcTemplate.queryForList(sql);

                List<Document> documents = items.stream().map(item -> {
                    String title = (String) item.get("title");
                    String content = "商品名称: " + title;
                    Map<String, Object> metadata = Map.of("id", item.get("id"), "title", title);
                    return new Document(content, metadata);
                }).collect(Collectors.toList());

                if (!documents.isEmpty()) {
                    vectorStore.add(documents);
                    System.out.println("[RAG] 向量知识库构建完成，共 " + documents.size() + " 个商品");
                }
                ready = true;
            } catch (Exception e) {
                System.err.println("[RAG] 向量库构建失败（不影响其他功能）: " + e.getMessage());
                ready = true; // 即使失败也标记为完成，避免 AI 接口一直等待
            }
        });
    }

    public String chatWithRag(String userMessage) {
        if (!ready) {
            return "AI 助手正在后台初始化知识库，请稍后再试（通常需要 1-2 分钟）";
        }
        List<Document> similarDocuments = vectorStore.similaritySearch(userMessage);

        if (similarDocuments.isEmpty()) {
            return "抱歉，根据您的描述，我们店里没有找到相关的商品。";
        }

        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

        System.out.println("向量检索召回的背景知识：\n" + context);

        String promptText = """
                你是一个名叫 AmazonRec 的资深电商智能导购。
                请严格根据以下【商店真实库存信息】来回答用户的提问。
                
                ⚠️ 核心规则：
                1. 绝不能捏造库存里没有的商品。
                2. 当你需要推荐商品时，必须按照以下严格的 Markdown 格式输出，以便前端渲染卡片：
                   [商品名称](product://商品ID)
                   例如：[《百年孤独》](product://101)
                
                【商店真实库存信息】：
                {context}
                
                用户的提问是：{question}
                """;

        PromptTemplate template = new PromptTemplate(promptText);
        template.add("context", context);
        template.add("question", userMessage);

        return chatClient.prompt(template.create()).call().content();
    }
}
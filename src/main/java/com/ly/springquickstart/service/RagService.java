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
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RagService(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = new SimpleVectorStore(embeddingModel);
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initKnowledgeBase() {
        System.out.println("正在构建商品向量知识库...");

        String sql = "SELECT id, title FROM items";
        List<Map<String, Object>> items = jdbcTemplate.queryForList(sql);

        List<Document> documents = items.stream().map(item -> {
            String title = (String) item.get("title");
            String content = "商品名称: " + title;
            Map<String, Object> metadata = Map.of("id", item.get("id"), "title", title);
            return new Document(content, metadata);
        }).collect(Collectors.toList());

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            System.out.println("成功将 " + documents.size() + " 个商品转换为高维向量并入库！");
        }
    }

    public String chatWithRag(String userMessage) {
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
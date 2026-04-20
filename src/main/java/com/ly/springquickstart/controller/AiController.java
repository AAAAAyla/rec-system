package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private RagService ragService;

    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }

        try {
            // 直接调用我们的 RAG 引擎！
            String aiReply = ragService.chatWithRag(userMessage);
            return Result.success(aiReply);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI 大脑暂时短路了：" + e.getMessage());
        }
    }
}
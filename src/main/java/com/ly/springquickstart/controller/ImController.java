package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.ImService;
import com.ly.springquickstart.service.MerchantService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/im")
public class ImController {

    @Autowired private ImService imService;
    @Autowired private MerchantService merchantService;

    /** 获取会话列表（买家或商家） */
    @GetMapping("/sessions")
    public Result sessions(@RequestParam(defaultValue = "user") String role) {
        Long userId = uid();
        Long merchantId = null;
        String userType = "user";

        if ("merchant".equals(role)) {
            merchantId = merchantService.getMerchantIdByUser(userId);
            userType = "merchant";
        }

        return Result.success(imService.getSessions(userType, userId, merchantId));
    }

    /** 创建或获取会话（买家发起） */
    @PostMapping("/sessions")
    public Result createSession(@RequestBody Map<String, Object> body) {
        Long userId = uid();
        Long merchantId = Long.parseLong(body.get("merchantId").toString());
        return Result.success(imService.getOrCreateSession(userId, merchantId));
    }

    /** 获取历史消息（分页） */
    @GetMapping("/sessions/{id}/messages")
    public Result messages(@PathVariable Long id,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "50") int pageSize) {
        return Result.success(imService.getMessages(id, pageNum, pageSize));
    }

    /** 发送消息（REST） */
    @PostMapping("/messages")
    public Result send(@RequestBody Map<String, Object> body) {
        Long sessionId = Long.parseLong(body.get("sessionId").toString());
        String senderType = (String) body.getOrDefault("senderType", "user");
        String content = (String) body.get("content");
        String type = (String) body.getOrDefault("type", "text");
        return Result.success(imService.sendMessage(sessionId, senderType, content, type));
    }

    /** 标记已读 */
    @PutMapping("/sessions/{id}/read")
    public Result markRead(@PathVariable Long id,
                           @RequestParam(defaultValue = "user") String role) {
        imService.markRead(id, role);
        return Result.success();
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }
}

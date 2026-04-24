package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.ImMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImService {

    @Autowired private ImMapper imMapper;

    public List<Map<String, Object>> getSessions(String userType, Long userId, Long merchantId) {
        return imMapper.findSessions(userType, userId, merchantId);
    }

    public Map<String, Object> getOrCreateSession(Long userId, Long merchantId) {
        Map<String, Object> session = imMapper.findSession(userId, merchantId);
        if (session == null) {
            session = new HashMap<>();
            session.put("userId", userId);
            session.put("merchantId", merchantId);
            imMapper.insertSession(session);
            session = imMapper.findSession(userId, merchantId);
        }
        return session;
    }

    public Map<String, Object> getMessages(Long sessionId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = imMapper.findMessages(sessionId, offset, pageSize);
        int total = imMapper.countMessages(sessionId);
        return Map.of("rows", rows, "total", total);
    }

    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, String senderType, String content, String type) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("sessionId", sessionId);
        msg.put("senderType", senderType);
        msg.put("content", content);
        msg.put("type", type != null ? type : "text");
        imMapper.insertMessage(msg);
        imMapper.updateSessionLastMessage(sessionId, content, senderType);
        return msg;
    }

    public void markRead(Long sessionId, String userType) {
        if ("user".equals(userType)) {
            imMapper.clearUserUnread(sessionId);
        } else {
            imMapper.clearMerchantUnread(sessionId);
        }
    }
}

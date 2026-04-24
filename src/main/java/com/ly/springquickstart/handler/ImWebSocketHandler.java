package com.ly.springquickstart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ly.springquickstart.service.ImService;
import com.ly.springquickstart.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @Autowired private ImService imService;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String key = getSessionKey(session);
        if (key != null) {
            sessions.put(key, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        Long sessionId = Long.parseLong(payload.get("sessionId").toString());
        String senderType = (String) payload.getOrDefault("senderType", "user");
        String content = (String) payload.get("content");
        String type = (String) payload.getOrDefault("type", "text");

        Map<String, Object> saved = imService.sendMessage(sessionId, senderType, content, type);

        String json = objectMapper.writeValueAsString(saved);
        TextMessage out = new TextMessage(json);

        Map<String, Object> sess = imService.getMessages(sessionId, 1, 1);
        // Broadcast to both parties of the session
        broadcastToSession(sessionId, out);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String key = getSessionKey(session);
        if (key != null) sessions.remove(key);
    }

    private void broadcastToSession(Long sessionId, TextMessage message) {
        sessions.values().forEach(ws -> {
            try {
                if (ws.isOpen()) {
                    String wsSessionId = ws.getAttributes().get("imSessionId") != null
                            ? ws.getAttributes().get("imSessionId").toString() : null;
                    // Send to all connected clients - they filter by sessionId on client side
                    ws.sendMessage(message);
                }
            } catch (IOException ignored) {}
        });
    }

    private String getSessionKey(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String token = UriComponentsBuilder.fromUri(uri).build()
                .getQueryParams().getFirst("token");
        if (token == null) return null;
        try {
            Map<String, Object> claims = JwtUtils.parseToken(token);
            String key = "ws:" + claims.get("id");
            return key;
        } catch (Exception e) {
            return null;
        }
    }
}

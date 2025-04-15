package com.example.SmartStay.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    public void sendMessageToAllClients(String event, List<Object> data) {
        try {
            String message = createMessage(event, data);
            for (WebSocketSession session : sessions) {
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createMessage(String event, List<Object> data) throws JsonProcessingException {
        CustomMessage customMessage = new CustomMessage(event, data);
        return objectMapper.writeValueAsString(customMessage);
    }

    @Setter
    @Getter
    private static class CustomMessage {
        private String event;
        private List<Object> data;

        public CustomMessage(String event, List<Object> data) {
            this.event = event;
            this.data = data;
        }
    }
}
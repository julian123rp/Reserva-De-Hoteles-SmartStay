package com.example.SmartStay.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebSocketService {

    private final WebSocketHandler webSocketHandler;

    @Autowired
    public WebSocketService(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void sendMessage(String eventName, List<Object> arguments) {
        webSocketHandler.sendMessageToAllClients(eventName, arguments);
    }
}
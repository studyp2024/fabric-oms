package com.oms.audit.config;

import com.oms.audit.service.WebSSHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

@Component
public class WebSSHHandler extends TextWebSocketHandler {

    @Autowired
    private WebSSHService webSSHService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null && query.contains("serverId=")) {
                String[] params = query.split("&");
                Long serverId = null;
                Long userId = null;
                for (String param : params) {
                    if (param.startsWith("serverId=")) {
                        serverId = Long.parseLong(param.split("=")[1]);
                    } else if (param.startsWith("userId=")) {
                        userId = Long.parseLong(param.split("=")[1]);
                    }
                }
                if (serverId != null) {
                    webSSHService.connect(session, serverId, userId);
                    return;
                }
            }
        }
        session.sendMessage(new TextMessage("Error: Missing serverId parameter.\r\n"));
        session.close();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        webSSHService.recvHandle(session, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        webSSHService.close(session);
    }
}

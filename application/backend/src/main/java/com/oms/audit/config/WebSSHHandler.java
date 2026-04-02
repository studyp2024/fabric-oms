package com.oms.audit.config;

import com.oms.audit.service.WebSSHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * WebSocket 文本消息处理器，接收并分发前端 xterm.js 传递过来的终端数据
 */
@Component
public class WebSSHHandler extends TextWebSocketHandler {

    @Autowired
    private WebSSHService webSSHService;

    /**
     * 当 WebSocket 连接成功建立后触发
     * 自动从 URL 参数中解析出目标 serverId，并初始化 SSH 连接
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null && query.contains("serverId=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("serverId=")) {
                        Long serverId = Long.parseLong(param.split("=")[1]);
                        // 初始化 SSH 桥接服务
                        webSSHService.connect(session, serverId);
                        return;
                    }
                }
            }
        }
        session.sendMessage(new TextMessage("Error: Missing serverId parameter.\r\n"));
        session.close();
    }

    /**
     * 处理从前端 WebSocket 传递过来的输入字符（按键事件）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        webSSHService.recvHandle(session, message.getPayload());
    }

    /**
     * 当 WebSocket 断开时，同时断开后端的 SSH 会话
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        webSSHService.close(session);
    }
}

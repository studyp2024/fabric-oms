package com.oms.audit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Spring Boot WebSocket 全局配置类
 * 开启 WebSocket 支持，并注册自定义的 WebSSH 消息处理器
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSSHHandler webSSHHandler;

    /**
     * 注册 WebSocket 处理器，设置连接的 endpoint 和允许的跨域来源
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 提供给前端 xterm.js 连接的后端接口路径为 /api/ws/ssh
        registry.addHandler(webSSHHandler, "/api/ws/ssh")
                .setAllowedOrigins("*"); // 允许跨域连接（实际生产应限制具体的来源）
    }
}

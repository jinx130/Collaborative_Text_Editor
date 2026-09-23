package com.yourname.editor.config;

import com.yourname.editor.collaboration.CollaborationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * Exposes the OT WebSocket at /ws/collab (proxied by Vite in dev).
 * The handshake passes Spring Security (login required) and copies the
 * HTTP session into the WS attributes, so the handler can resolve the
 * account even if the principal ever comes back null.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CollaborationWebSocketHandler handler;
    private final String frontendUrl;

    public WebSocketConfig(CollaborationWebSocketHandler handler,
            @Value("${FRONTEND_URL:https://change-me.vercel.app}") String frontendUrl) {
        this.handler = handler;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/collab")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("http://localhost:5173", frontendUrl);
    }
}

package bo.cdr.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrPollingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrReceivingTransportHandler;
import org.springframework.web.socket.sockjs.transport.handler.XhrStreamingTransportHandler;

import java.util.Arrays;

/**
 * STOMP + SockJS como la referencia (mismos transportes). NUEVO: el canal de entrada pasa por
 * WebSocketAuthInterceptor (JWT en CONNECT, rol en SUBSCRIBE). Broker simple en memoria: una sola
 * réplica del backend (ver docs/architecture/realtime.md §6).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws-cdr";

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addEndpoint(ENDPOINT)
                .setAllowedOriginPatterns(origins)
                .withSockJS()
                .setTransportHandlers(
                        new WebSocketTransportHandler(new DefaultHandshakeHandler()),
                        new XhrStreamingTransportHandler(),
                        new XhrPollingTransportHandler(),
                        new XhrReceivingTransportHandler());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}

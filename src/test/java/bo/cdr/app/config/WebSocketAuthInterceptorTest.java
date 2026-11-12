package bo.cdr.app.config;

import bo.cdr.app.appsecurity.auth.jwt.JwtAuthenticationFilter;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.util.constants.RoleCodes;
import bo.cdr.app.util.constants.WebSocketTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock JwtService jwtService;
    @Mock JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock MessageChannel channel;
    WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService, jwtAuthenticationFilter);
    }

    private static UsernamePasswordAuthenticationToken user(String role) {
        return new UsernamePasswordAuthenticationToken("u", null, List.of(new SimpleGrantedAuthority(role)));
    }

    private static Message<byte[]> frame(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return frame(accessor);
    }

    private static Message<byte[]> subscribe(String destination, UsernamePasswordAuthenticationToken user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return frame(accessor);
    }

    @Test
    void connect_setsUser_whenTokenValid() {
        UsernamePasswordAuthenticationToken auth = user(RoleCodes.COCINA);
        when(jwtService.extractBearer("Bearer abc")).thenReturn("abc");
        when(jwtService.validateToken("abc")).thenReturn(true);
        when(jwtAuthenticationFilter.buildAuthentication("abc")).thenReturn(auth);

        Message<?> result = interceptor.preSend(connect("Bearer abc"), channel);

        assertThat(StompHeaderAccessor.wrap(result).getUser()).isSameAs(auth);
    }

    @Test
    void connect_rejects_whenUserDeactivatedAfterTokenWasIssued() {
        when(jwtService.extractBearer("Bearer abc")).thenReturn("abc");
        when(jwtService.validateToken("abc")).thenReturn(true);
        when(jwtAuthenticationFilter.buildAuthentication("abc")).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer abc"), channel)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connect_rejects_whenTokenMissingOrInvalid() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), channel)).isInstanceOf(AccessDeniedException.class);

        when(jwtService.extractBearer("Bearer bad")).thenReturn("bad");
        when(jwtService.validateToken("bad")).thenReturn(false);
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer bad"), channel)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscribe_ordersAllowedForKitchen_catalogNot() {
        interceptor.preSend(subscribe(WebSocketTopics.ORDERS, user(RoleCodes.COCINA)), channel);
        assertThatThrownBy(() -> interceptor.preSend(subscribe(WebSocketTopics.CATALOG, user(RoleCodes.COCINA)), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscribe_catalogAllowedForCashier() {
        interceptor.preSend(subscribe(WebSocketTopics.CATALOG, user(RoleCodes.CAJERO)), channel);
    }

    @Test
    void subscribe_rejectsUnknownTopic_andAnonymous() {
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/otro", user(RoleCodes.ADMIN)), channel))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(subscribe(WebSocketTopics.ORDERS, null), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void send_isRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/x");
        assertThatThrownBy(() -> interceptor.preSend(frame(accessor), channel)).isInstanceOf(AccessDeniedException.class);
    }
}

package com.medilinkpro.backend.config;

import com.medilinkpro.backend.security.jwt.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal temps reel utilise pour diffuser les alertes de soins a domicile (F-Alertes) :
 * - /topic/alertes            : broadcast aux infirmieres connectees (nouvelle alerte, alerte prise)
 * - /user/queue/alertes       : messages prives a un patient (sa propre alerte a ete prise)
 *
 * L'authentification se fait au niveau de la frame STOMP CONNECT (header Authorization),
 * pas au niveau HTTP : la poignee de main SockJS initiale reste publique (voir SecurityConfig),
 * voir StompAuthChannelInterceptor pour le detail de la validation du JWT et des autorisations.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-alertes")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}

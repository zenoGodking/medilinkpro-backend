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
 * - /user/queue/alertes       : messages prives (alerte proposee a une infirmiere proche, suivi de son alerte pour un patient)
 * - /user/queue/suivi         : position en temps reel de l'infirmiere en route, pour le patient
 * - /app/infirmiers/position  : envoi de la position GPS par l'infirmiere (PositionInfirmierController)
 * - /app/teleconsultation/{rdv}/signal + /user/queue/teleconsultation : signalisation WebRTC des
 *   teleconsultations video (TeleconsultationController)
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

    @org.springframework.beans.factory.annotation.Value("${medilinkpro.cors.origines:}")
    private String originesCors;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-alertes")
                .setAllowedOriginPatterns(java.util.Arrays.stream(originesCors.split(","))
                        .map(String::trim).filter(o -> !o.isEmpty()).toArray(String[]::new))
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

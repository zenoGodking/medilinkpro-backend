package com.medilinkpro.backend.security.jwt;

import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Authentifie chaque connexion WebSocket au moment de la frame STOMP CONNECT en lisant
 * le JWT depuis l'en-tete "Authorization" (le client le passe en connectHeaders), et
 * restreint l'abonnement au flux d'alertes ("/topic/alertes") aux seules infirmieres,
 * puisque les alertes contiennent des donnees personnelles du patient (adresse, telephone).
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Token d'authentification manquant");
            }

            String token = authHeader.substring(7);
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new IllegalArgumentException("Token d'authentification invalide ou expire");
            }

            Principal principal = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(principal);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();

            if ("/topic/alertes".equals(destination)) {
                boolean estInfirmier = principal instanceof UsernamePasswordAuthenticationToken auth
                        && auth.getPrincipal() instanceof Utilisateur u
                        && u.getRole() == Role.INFIRMIER;

                if (!estInfirmier) {
                    throw new IllegalArgumentException("Abonnement reserve aux infirmieres");
                }
            }
        }

        return message;
    }
}

package com.medilinkpro.backend;

import com.medilinkpro.backend.dto.request.AlerteRequest;
import com.medilinkpro.backend.dto.response.AlerteResponse;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import com.medilinkpro.backend.service.AlerteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bout en bout sur le vrai canal WebSocket (STOMP/SockJS) : notification ciblee de
 * l'infirmiere proche, puis relais de sa position au patient pendant l'intervention.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SuiviTempsReelWebSocketTest {

    @LocalServerPort int port;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired JwtService jwtService;
    @Autowired AlerteService alerteService;

    @Test
    @SuppressWarnings("unchecked")
    void infirmiereProcheNotifieeEtPatientRecoitSaPositionEnDirect() throws Exception {
        Patient patient = utilisateurRepository.save(Patient.builder()
                .nom("Mballa").prenom("Aline").email("ws-patient@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        Infirmier infirmiere = utilisateurRepository.save(Infirmier.builder()
                .nom("Ngo").prenom("Carine").email("ws-inf@test.cm").motDePasse("x")
                .role(Role.INFIRMIER).statutCompte(StatutCompte.APPROUVE).actif(true)
                .latitude(3.8570).longitude(11.5021).datePosition(LocalDateTime.now()).build());

        StompSession sessionInf = connecter(infirmiere);
        StompSession sessionPatient = connecter(patient);
        BlockingQueue<Map<String, Object>> alertesInf = abonner(sessionInf, "/user/queue/alertes");
        BlockingQueue<Map<String, Object>> suiviPatient = abonner(sessionPatient, "/user/queue/suivi");
        Thread.sleep(500); // laisse les abonnements s'enregistrer

        AlerteResponse alerte = alerteService.creerAlerte(patient.getId(),
                AlerteRequest.builder().adresse("Bastos").latitude(3.8480).longitude(11.5021).build());

        Map<String, Object> notif = alertesInf.poll(5, TimeUnit.SECONDS);
        assertThat(notif).as("l'infirmiere proche est notifiee en prive").isNotNull();
        assertThat(notif.get("id")).isEqualTo(alerte.getId().toString());
        assertThat(((Number) notif.get("distanceKm")).doubleValue()).isBetween(0.9, 1.1);

        alerteService.repondre(alerte.getId(), infirmiere.getId());

        sessionInf.send("/app/infirmiers/position", Map.of("latitude", 3.8525, "longitude", 11.5021));

        Map<String, Object> position = suiviPatient.poll(5, TimeUnit.SECONDS);
        assertThat(position).as("le patient recoit la position en temps reel").isNotNull();
        assertThat(((Number) position.get("latitude")).doubleValue()).isEqualTo(3.8525);
        assertThat(((Number) position.get("distanceKm")).doubleValue()).isBetween(0.4, 0.6);

        sessionInf.disconnect();
        sessionPatient.disconnect();
    }

    private StompSession connecter(Utilisateur u) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtService.generateToken(
                utilisateurRepository.findById(u.getId()).orElseThrow()));
        return client.connectAsync("http://localhost:" + port + "/ws-alertes", new WebSocketHttpHeaders(),
                        connectHeaders, new StompSessionHandlerAdapter() { })
                .get(10, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> abonner(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> recus = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                recus.add((Map<String, Object>) payload);
            }
        });
        return recus;
    }
}

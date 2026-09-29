package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.AbonnementPush;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.entity.suivi.RappelMedicament;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.AbonnementPushRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.repository.suivi.RappelMedicamentRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import com.medilinkpro.backend.service.push.CleVapidService;
import com.medilinkpro.backend.service.push.ExpediteurPush;
import com.medilinkpro.backend.service.push.RappelsMedicamentsPlanificateur;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NotificationPushTest {

    /** Remplace l'envoi reseau : capture les messages (endpoint -> contenu). */
    static final List<String[]> ENVOYES = Collections.synchronizedList(new ArrayList<>());
    static final AtomicInteger CODE_REPONSE = new AtomicInteger(201);

    @TestConfiguration
    static class FauxExpediteur {
        @Bean
        @Primary
        ExpediteurPush expediteurDeTest() {
            return (abonnement, json) -> {
                ENVOYES.add(new String[]{abonnement.getEndpoint(), json});
                return CODE_REPONSE.get();
            };
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired AbonnementPushRepository abonnementRepository;
    @Autowired RappelMedicamentRepository rappelRepository;
    @Autowired RappelsMedicamentsPlanificateur planificateur;
    @Autowired CleVapidService cleVapidService;
    @Autowired JwtService jwtService;

    private Patient aline;
    private Infirmier infirmiere;

    @BeforeEach
    void preparer() {
        ENVOYES.clear();
        CODE_REPONSE.set(201);
        aline = utilisateurRepository.save(Patient.builder().nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        infirmiere = utilisateurRepository.save(Infirmier.builder().nom("Ngo").prenom("Carine").email("carine@test.cm").motDePasse("x")
                .role(Role.INFIRMIER).statutCompte(StatutCompte.APPROUVE).actif(true)
                .latitude(3.857).longitude(11.5021).datePosition(LocalDateTime.now()).build());
    }

    @Test
    void cleVapidStableEtChiffrementReel() throws Exception {
        String cle = json(appeler(get("/api/push/cle-publique"), aline)).get("clePublique").asText();
        assertThat(Base64.getUrlDecoder().decode(cle)).hasSize(65); // point P-256 non compresse
        assertThat(json(appeler(get("/api/push/cle-publique"), infirmiere)).get("clePublique").asText()).isEqualTo(cle);

        // Chiffrement et signature VAPID reels pour un abonnement de type navigateur (sans envoi reseau)
        KeyPairGenerator g = KeyPairGenerator.getInstance("ECDH", "BC");
        g.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
        KeyPair navigateur = g.generateKeyPair();
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        Notification n = new Notification("https://fcm.googleapis.com/fcm/send/abc",
                b64.encodeToString(Utils.encode((ECPublicKey) navigateur.getPublic())), b64.encodeToString(auth), "{\"titre\":\"x\"}");
        String[] cles = cleVapidService.cles();
        var requete = new PushService(cles[0], cles[1], "mailto:test@medilinkpro.cm").preparePost(n, Encoding.AES128GCM);
        assertThat(requete.getFirstHeader("Authorization").getValue()).startsWith("vapid t=");
        assertThat(requete.getFirstHeader("Content-Encoding").getValue()).isEqualTo("aes128gcm");
        assertThat(requete.getEntity().getContentLength()).isGreaterThan(86);
    }

    @Test
    void infirmiereProcheEtPatientNotifies() throws Exception {
        abonner(infirmiere, "https://push.example/infirmiere");
        abonner(aline, "https://push.example/aline");

        String alerteId = json(envoyer(post("/api/alertes/patients/" + aline.getId()),
                Map.of("adresse", "Bastos", "latitude", 3.848, "longitude", 11.5021), aline)
                .andExpect(status().isCreated())).get("id").asText();
        String[] pourInfirmiere = attendreEnvoi("https://push.example/infirmiere");
        assertThat(pourInfirmiere[1]).contains("Demande de soins a domicile").contains("1,0 km").contains("Bastos");

        appeler(patch("/api/alertes/" + alerteId + "/repondre").param("infirmierId", infirmiere.getId().toString()), infirmiere)
                .andExpect(status().isOk());
        assertThat(attendreEnvoi("https://push.example/aline")[1]).contains("Une infirmiere arrive").contains("Carine Ngo");
    }

    @Test
    void rappelDeMedicamentUneSeuleFoisALHeure() throws Exception {
        abonner(aline, "https://push.example/aline");
        rappelRepository.save(RappelMedicament.builder().patient(aline).medicament("Metformine").dosage("500 mg")
                .heures(new ArrayList<>(List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)))).dateDebut(LocalDate.now()).build());
        LocalDate j = LocalDate.now();

        assertThat(planificateur.envoyerRappelsDus(j.atTime(7, 59))).isZero();
        assertThat(planificateur.envoyerRappelsDus(j.atTime(8, 1))).isEqualTo(1);
        assertThat(attendreEnvoi("https://push.example/aline")[1]).contains("Metformine (500 mg)");
        assertThat(planificateur.envoyerRappelsDus(j.atTime(8, 3))).isZero();   // deja envoye
        assertThat(planificateur.envoyerRappelsDus(j.atTime(8, 30))).isZero();  // hors fenetre : pas de rattrapage
        assertThat(planificateur.envoyerRappelsDus(j.atTime(20, 0))).isEqualTo(1);
    }

    @Test
    void abonnementExpireSupprimeEtEndpointInvalideRefuse() throws Exception {
        envoyer(post("/api/push/abonnements"), Map.of("endpoint", "http://pas-https", "keys", Map.of("p256dh", "x", "auth", "y")), aline)
                .andExpect(status().isBadRequest());
        abonner(aline, "https://push.example/perime");
        CODE_REPONSE.set(410);
        rappelRepository.save(RappelMedicament.builder().patient(aline).medicament("Aspirine")
                .heures(new ArrayList<>(List.of(LocalTime.of(9, 0)))).dateDebut(LocalDate.now()).build());
        planificateur.envoyerRappelsDus(LocalDate.now().atTime(9, 0));
        attendreEnvoi("https://push.example/perime");
        for (int i = 0; i < 50 && !abonnementRepository.findByUtilisateurId(aline.getId()).isEmpty(); i++) Thread.sleep(100);
        assertThat(abonnementRepository.findByUtilisateurId(aline.getId())).isEmpty();
    }

    private void abonner(Utilisateur u, String endpoint) throws Exception {
        envoyer(post("/api/push/abonnements"), Map.of("endpoint", endpoint, "keys", Map.of("p256dh", "cle", "auth", "secret")), u)
                .andExpect(status().isNoContent());
        assertThat(abonnementRepository.findByEndpoint(endpoint)).map(AbonnementPush::getUtilisateurId).contains(u.getId());
    }

    private String[] attendreEnvoi(String endpoint) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            synchronized (ENVOYES) {
                for (String[] e : ENVOYES) if (e[0].equals(endpoint)) return e;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Aucune notification envoyee a " + endpoint);
    }

    private ResultActions envoyer(MockHttpServletRequestBuilder requete, Object corps, Utilisateur u) throws Exception {
        return appeler(requete.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corps)), u);
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, Utilisateur u) throws Exception {
        return mockMvc.perform(requete.header("Authorization",
                "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow())));
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }
}

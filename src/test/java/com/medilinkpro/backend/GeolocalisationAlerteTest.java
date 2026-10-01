package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.AlerteSoinDomicile;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.AlerteSoinDomicileRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import com.medilinkpro.backend.service.AlerteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class GeolocalisationAlerteTest {

    // Patient a Yaounde centre ; 0.01 degre de latitude ~ 1.1 km
    private static final double LAT = 3.8480;
    private static final double LNG = 11.5021;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired AlerteSoinDomicileRepository alerteRepository;
    @Autowired AlerteService alerteService;
    @Autowired JwtService jwtService;

    private Patient patient;
    private final List<Infirmier> proches = new ArrayList<>();
    private Infirmier lointaine;
    private Infirmier horsLigne;

    @BeforeEach
    void preparer() {
        patient = utilisateurRepository.save(Patient.builder()
                .nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        // 7 infirmieres a 1, 2, ... 7 km (au nord du patient)
        for (int i = 1; i <= 7; i++) {
            proches.add(infirmiere("inf" + i, LAT + i * 0.009, LNG, LocalDateTime.now()));
        }
        lointaine = infirmiere("loin", LAT + 0.5, LNG, LocalDateTime.now());           // ~55 km
        horsLigne = infirmiere("horsligne", LAT + 0.001, LNG, LocalDateTime.now().minusHours(2)); // tres proche mais app fermee
    }

    @Test
    void alerteNotifieeAuxCinqInfirmieresLesPlusProchesPuisElargie() throws Exception {
        JsonNode alerte = creerAlerte(LAT, LNG);
        assertThat(alerte.get("nombreInfirmiersNotifies").asInt()).isEqualTo(5);
        assertThat(alerte.get("diffusionGenerale").asBoolean()).isFalse();

        AlerteSoinDomicile enBase = alerteRepository.findById(UUID.fromString(alerte.get("id").asText())).orElseThrow();
        assertThat(enBase.getInfirmiersNotifies())
                .containsExactlyInAnyOrderElementsOf(proches.subList(0, 5).stream().map(Infirmier::getId).toList())
                .doesNotContain(horsLigne.getId(), lointaine.getId());

        // La plus proche voit l'alerte avec sa distance ; la 6e ne la voit pas encore
        JsonNode vuesParInf1 = lister(proches.get(0));
        assertThat(vuesParInf1).hasSize(1);
        assertThat(vuesParInf1.get(0).get("distanceKm").asDouble()).isBetween(0.9, 1.1);
        assertThat(lister(proches.get(5))).isEmpty();

        // Personne ne repond : vague suivante (6e et 7e), puis diffusion generale
        vieillir(enBase.getId());
        alerteService.elargirAlertesSansReponse();
        assertThat(alerteRepository.findById(enBase.getId()).orElseThrow().getInfirmiersNotifies()).hasSize(7);
        assertThat(lister(proches.get(5))).hasSize(1);

        vieillir(enBase.getId());
        alerteService.elargirAlertesSansReponse();
        assertThat(alerteRepository.findById(enBase.getId()).orElseThrow().isDiffusionGenerale()).isTrue();
        assertThat(lister(lointaine)).hasSize(1);
    }

    @Test
    void seuleUneInfirmiereNotifieePeutRepondre() throws Exception {
        String alerteId = creerAlerte(LAT, LNG).get("id").asText();

        mockMvc.perform(patch("/api/alertes/" + alerteId + "/repondre")
                        .param("infirmierId", proches.get(6).getId().toString())
                        .header("Authorization", bearer(proches.get(6))))
                .andExpect(status().isConflict());

        String reponse = mockMvc.perform(patch("/api/alertes/" + alerteId + "/repondre")
                        .param("infirmierId", proches.get(0).getId().toString())
                        .header("Authorization", bearer(proches.get(0))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode r = objectMapper.readTree(reponse);
        assertThat(r.get("statut").asText()).isEqualTo("REPONDUE");
        assertThat(r.get("infirmierTelephone").asText()).isEqualTo("+237699000000");
    }

    @Test
    void patientSuitLaPositionDeLInfirmiereEnRoute() throws Exception {
        String alerteId = creerAlerte(LAT, LNG).get("id").asText();
        Infirmier inf = proches.get(0);
        mockMvc.perform(patch("/api/alertes/" + alerteId + "/repondre")
                        .param("infirmierId", inf.getId().toString())
                        .header("Authorization", bearer(inf)))
                .andExpect(status().isOk());

        // L'infirmiere se rapproche (~0.5 km)
        mockMvc.perform(put("/api/alertes/infirmiers/moi/position")
                        .header("Authorization", bearer(inf))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("latitude", LAT + 0.0045, "longitude", LNG))))
                .andExpect(status().isNoContent());

        String suivi = mockMvc.perform(get("/api/alertes/" + alerteId + "/suivi").header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode s = objectMapper.readTree(suivi);
        assertThat(s.get("latitude").asDouble()).isEqualTo(LAT + 0.0045);
        assertThat(s.get("distanceKm").asDouble()).isBetween(0.4, 0.6);

        // Un autre patient ne peut pas suivre cette infirmiere
        Patient autre = utilisateurRepository.save(Patient.builder()
                .nom("X").prenom("Y").email("autre@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        mockMvc.perform(get("/api/alertes/" + alerteId + "/suivi").header("Authorization", bearer(autre)))
                .andExpect(status().isForbidden());
    }

    @Test
    void impossibleDAgirAuNomDUnAutreUtilisateur() throws Exception {
        String alerteId = creerAlerte(LAT, LNG).get("id").asText();
        Patient autre = utilisateurRepository.save(Patient.builder()
                .nom("X").prenom("Y").email("usurpateur@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());

        // Un autre patient ne peut ni lire l'historique, ni annuler, ni creer une alerte au nom d'Aline
        mockMvc.perform(get("/api/alertes/patients/" + patient.getId()).header("Authorization", bearer(autre)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/alertes/" + alerteId + "/annuler")
                        .param("patientId", patient.getId().toString())
                        .header("Authorization", bearer(autre)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/alertes/patients/" + patient.getId())
                        .header("Authorization", bearer(autre))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("adresse", "Bastos"))))
                .andExpect(status().isForbidden());

        // Une infirmiere (non notifiee) ne peut pas repondre en se faisant passer pour la plus proche
        mockMvc.perform(patch("/api/alertes/" + alerteId + "/repondre")
                        .param("infirmierId", proches.get(0).getId().toString())
                        .header("Authorization", bearer(proches.get(6))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/alertes/infirmiers/" + proches.get(0).getId() + "/en-cours")
                        .header("Authorization", bearer(proches.get(6))))
                .andExpect(status().isForbidden());

        // L'alerte est intacte : toujours en attente
        assertThat(alerteRepository.findById(UUID.fromString(alerteId)).orElseThrow().getStatut().name())
                .isEqualTo("EN_ATTENTE");
    }

    @Test
    void sansPositionDuPatientDiffusionGenerale() throws Exception {
        JsonNode alerte = objectMapper.readTree(mockMvc.perform(post("/api/alertes/patients/" + patient.getId())
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("adresse", "Bastos"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertThat(alerte.get("diffusionGenerale").asBoolean()).isTrue();
        assertThat(lister(lointaine)).hasSize(1);
    }

    private Infirmier infirmiere(String nom, double lat, double lng, LocalDateTime datePosition) {
        return utilisateurRepository.save(Infirmier.builder()
                .nom(nom).prenom("Inf").email(nom + "@test.cm").motDePasse("x").telephone("+237699000000")
                .role(Role.INFIRMIER).photoProfilChemin("photos-infirmiers/test.jpg").statutCompte(StatutCompte.APPROUVE).actif(true)
                .latitude(lat).longitude(lng).datePosition(datePosition).build());
    }

    private JsonNode creerAlerte(double lat, double lng) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("adresse", "Bastos", "latitude", lat, "longitude", lng));
        return objectMapper.readTree(mockMvc.perform(post("/api/alertes/patients/" + patient.getId())
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode lister(Infirmier inf) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/alertes/actives").header("Authorization", bearer(inf)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private void vieillir(UUID alerteId) {
        AlerteSoinDomicile a = alerteRepository.findById(alerteId).orElseThrow();
        a.setDateDerniereDiffusion(LocalDateTime.now().minusMinutes(10));
        alerteRepository.save(a);
    }

    private String bearer(Utilisateur u) {
        return "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow());
    }
}

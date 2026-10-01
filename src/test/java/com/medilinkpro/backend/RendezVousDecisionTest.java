package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.repository.RendezVousRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import com.medilinkpro.backend.service.RappelsRendezVousPlanificateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Demande de rendez-vous, decision du medecin (accepter / refuser / reporter), notifications, rappels et avis. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RendezVousDecisionTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired RendezVousRepository rendezVousRepository;
    @Autowired RappelsRendezVousPlanificateur rappels;
    @Autowired JwtService jwtService;

    private Medecin medecin;
    private Medecin autreMedecin;
    private Patient patient;
    private final LocalDate lundi = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    @BeforeEach
    void preparer() {
        medecin = utilisateurRepository.save(Medecin.builder().nom("Kamga").prenom("Paul").email("kamga@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        autreMedecin = utilisateurRepository.save(Medecin.builder().nom("Etoa").prenom("Jean").email("etoa@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        patient = utilisateurRepository.save(Patient.builder().nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
    }

    @Test
    void leMedecinAccepteRefuseOuReporteEtLePatientEstPrevenu() throws Exception {
        String rdv1 = demander(lundi.atTime(9, 0));
        assertThat(json(appeler(get("/api/rendez-vous/" + rdv1), patient)).get("statut").asText()).isEqualTo("EN_ATTENTE");
        assertThat(titres(medecin)).contains("Nouvelle demande de rendez-vous");

        appeler(patch("/api/rendez-vous/" + rdv1 + "/accepter"), autreMedecin).andExpect(status().isForbidden());
        appeler(patch("/api/rendez-vous/" + rdv1 + "/accepter"), patient).andExpect(status().isForbidden());
        assertThat(json(appeler(patch("/api/rendez-vous/" + rdv1 + "/accepter"), medecin).andExpect(status().isOk()))
                .get("statut").asText()).isEqualTo("CONFIRME");
        assertThat(titres(patient)).contains("Rendez-vous confirmé");

        // Report sur un autre creneau libre
        appeler(patch("/api/rendez-vous/" + rdv1 + "/reporter").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("nouvelleDateHeure", lundi.atTime(12, 30).toString()))), medecin)
                .andExpect(status().isBadRequest()); // hors heures de consultation
        JsonNode reporte = json(appeler(patch("/api/rendez-vous/" + rdv1 + "/reporter").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("nouvelleDateHeure", lundi.atTime(15, 0).toString(), "motif", "Urgence au bloc"))), medecin)
                .andExpect(status().isOk()));
        assertThat(reporte.get("dateHeure").asText()).startsWith(lundi + "T15:00");
        assertThat(reporte.get("dateHeureInitiale").asText()).startsWith(lundi + "T09:00");
        assertThat(reporte.get("motifMedecin").asText()).isEqualTo("Urgence au bloc");
        assertThat(titres(patient)).contains("Rendez-vous reporté");
        // L'ancien creneau est libere, le nouveau occupe
        demander(lundi.atTime(9, 0));
        appeler(post("/api/rendez-vous").contentType(MediaType.APPLICATION_JSON).content(corps(Map.of(
                "patientId", patient.getId(), "medecinId", medecin.getId(), "dateHeure", lundi.atTime(15, 0).toString()))), patient)
                .andExpect(status().isBadRequest());

        // Refus avec motif
        String rdv2 = demander(lundi.atTime(10, 0));
        assertThat(json(appeler(patch("/api/rendez-vous/" + rdv2 + "/refuser").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("motif", "Je ne suis pas disponible"))), medecin)).get("statut").asText()).isEqualTo("REFUSE");
        assertThat(titres(patient)).contains("Rendez-vous non disponible");
        demander(lundi.atTime(10, 0)); // creneau refuse = libre
        appeler(patch("/api/rendez-vous/" + rdv2 + "/accepter"), medecin).andExpect(status().isConflict());

        // Annulation par le patient : le medecin est prevenu
        appeler(patch("/api/rendez-vous/" + rdv1 + "/statut").contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"CONFIRME\"}"), patient).andExpect(status().isForbidden());
        appeler(patch("/api/rendez-vous/" + rdv1 + "/statut").contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"ANNULE\"}"), patient).andExpect(status().isOk());
        assertThat(titres(medecin)).contains("Rendez-vous annulé par le patient");

        // Lecture des notifications
        JsonNode nonLues = json(appeler(get("/api/notifications/non-lues"), patient));
        assertThat(nonLues.get("nombre").asLong()).isGreaterThanOrEqualTo(3);
        appeler(patch("/api/notifications/lues"), patient).andExpect(status().isNoContent());
        assertThat(json(appeler(get("/api/notifications/non-lues"), patient)).get("nombre").asLong()).isZero();
    }

    @Test
    void rappelsLaVeilleEtUneHeureAvantUneSeuleFois() throws Exception {
        LocalDateTime maintenant = LocalDateTime.of(2030, 1, 7, 8, 0);
        RendezVous demain = sauver(maintenant.plusHours(20), StatutRendezVous.CONFIRME);
        RendezVous bientot = sauver(maintenant.plusMinutes(45), StatutRendezVous.CONFIRME);
        sauver(maintenant.plusHours(2), StatutRendezVous.EN_ATTENTE); // non confirme : pas de rappel
        sauver(maintenant.plusDays(3), StatutRendezVous.CONFIRME);    // trop loin

        assertThat(rappels.envoyerRappels(maintenant)).isEqualTo(2);
        assertThat(rappels.envoyerRappels(maintenant)).isZero();
        assertThat(rendezVousRepository.findById(demain.getId()).orElseThrow().isRappelEnvoye()).isTrue();
        assertThat(rendezVousRepository.findById(bientot.getId()).orElseThrow().isRappelProcheEnvoye()).isTrue();

        // Une heure avant le rendez-vous de demain : second rappel
        assertThat(rappels.envoyerRappels(maintenant.plusHours(19).plusMinutes(30))).isEqualTo(1);
        assertThat(titres(patient)).contains("Rappel de rendez-vous", "Votre rendez-vous approche");
    }

    @Test
    void lePatientNoteLeMedecinApresLeRendezVous() throws Exception {
        RendezVous aVenir = sauver(LocalDateTime.now().plusDays(2), StatutRendezVous.CONFIRME);
        RendezVous passe = sauver(LocalDateTime.now().minusDays(1), StatutRendezVous.TERMINE);
        String avis = corps(Map.of("note", 4, "commentaire", "Tres a l'ecoute"));

        appeler(post("/api/rendez-vous/" + aVenir.getId() + "/avis").contentType(MediaType.APPLICATION_JSON).content(avis), patient)
                .andExpect(status().isBadRequest());
        appeler(post("/api/rendez-vous/" + passe.getId() + "/avis").contentType(MediaType.APPLICATION_JSON).content(avis), medecin)
                .andExpect(status().isForbidden());
        appeler(post("/api/rendez-vous/" + passe.getId() + "/avis").contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":9}"), patient).andExpect(status().isBadRequest());
        String avisId = json(appeler(post("/api/rendez-vous/" + passe.getId() + "/avis").contentType(MediaType.APPLICATION_JSON)
                .content(avis), patient).andExpect(status().isCreated())).get("id").asText();
        appeler(post("/api/rendez-vous/" + passe.getId() + "/avis").contentType(MediaType.APPLICATION_JSON).content(avis), patient)
                .andExpect(status().isConflict());

        JsonNode resume = json(appeler(get("/api/rendez-vous/avis/medecins/" + medecin.getId()), patient));
        assertThat(resume.get("moyenne").asDouble()).isEqualTo(4.0);
        assertThat(resume.get("avis").get(0).get("auteur").asText()).isEqualTo("Aline M.");
        assertThat(json(appeler(get("/api/medecins/" + medecin.getId()), patient)).get("nombreAvis").asLong()).isEqualTo(1);
        assertThat(json(appeler(get("/api/rendez-vous/patient/" + patient.getId()), patient)))
                .anySatisfy(r -> assertThat(r.get("avisDonne").asBoolean()).isTrue());

        // Moderation
        Utilisateur admin = utilisateurRepository.findByEmail("womeloic@gmail.com").orElseThrow();
        appeler(patch("/api/rendez-vous/avis/" + avisId + "/masquer"), patient).andExpect(status().isForbidden());
        appeler(patch("/api/rendez-vous/avis/" + avisId + "/masquer"), admin).andExpect(status().isOk());
        assertThat(json(appeler(get("/api/rendez-vous/avis/medecins/" + medecin.getId()), patient)).get("nombre").asLong()).isZero();
    }

    private RendezVous sauver(LocalDateTime quand, StatutRendezVous statut) {
        return rendezVousRepository.save(RendezVous.builder().patient(patient).medecin(medecin).dateHeure(quand).statut(statut).build());
    }

    private String demander(LocalDateTime quand) throws Exception {
        return json(appeler(post("/api/rendez-vous").contentType(MediaType.APPLICATION_JSON).content(corps(Map.of(
                "patientId", patient.getId(), "medecinId", medecin.getId(), "dateHeure", quand.toString()))), patient)
                .andExpect(status().isCreated())).get("id").asText();
    }

    private java.util.List<String> titres(Utilisateur u) throws Exception {
        java.util.List<String> titres = new java.util.ArrayList<>();
        json(appeler(get("/api/notifications"), u)).forEach(n -> titres.add(n.get("titre").asText()));
        return titres;
    }

    private String corps(Map<String, ?> valeurs) throws Exception {
        return objectMapper.writeValueAsString(valeurs);
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, Utilisateur u) throws Exception {
        return mockMvc.perform(requete.header("Authorization",
                "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow())));
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }
}

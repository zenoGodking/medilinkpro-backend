package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SuiviSanteTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired DossierMedicalRepository dossierMedicalRepository;
    @Autowired JwtService jwtService;

    private Patient aline;
    private Patient bruno;
    private Medecin medecin;
    private String base;

    @BeforeEach
    void preparer() {
        aline = patient("aline");
        bruno = patient("bruno");
        medecin = utilisateurRepository.save(Medecin.builder().nom("Kamga").prenom("Paul").email("kamga@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        base = "/api/suivi/patients/" + aline.getId();
    }

    @Test
    void mesuresAvecLectureIndicativeEtControleDePlausibilite() throws Exception {
        envoyer(post(base + "/mesures"), Map.of("type", "TENSION", "valeur", 165, "valeur2", 95), aline).andExpect(status().isCreated());
        envoyer(post(base + "/mesures"), Map.of("type", "TENSION", "valeur", 118, "valeur2", 76), aline).andExpect(status().isCreated());
        envoyer(post(base + "/mesures"), Map.of("type", "GLYCEMIE", "valeur", 0.55, "aJeun", true), aline).andExpect(status().isCreated());
        envoyer(post(base + "/mesures"), Map.of("type", "GLYCEMIE", "valeur", 1.6, "aJeun", false), aline).andExpect(status().isCreated());
        envoyer(post(base + "/mesures"), Map.of("type", "GLYCEMIE", "valeur", 1.6, "aJeun", true), aline).andExpect(status().isCreated());
        envoyer(post(base + "/mesures"), Map.of("type", "SATURATION_O2", "valeur", 88), aline).andExpect(status().isCreated());

        // Valeurs impossibles (erreur de saisie) refusees
        envoyer(post(base + "/mesures"), Map.of("type", "TEMPERATURE", "valeur", 375), aline).andExpect(status().isBadRequest());
        envoyer(post(base + "/mesures"), Map.of("type", "TENSION", "valeur", 80, "valeur2", 120), aline).andExpect(status().isBadRequest());

        JsonNode mesures = json(appeler(get(base + "/mesures"), aline));
        assertThat(mesures).hasSize(6);
        assertThat(niveaux(mesures)).containsExactly("ATTENTION", "NORMAL", "ALERTE", "NORMAL", "ATTENTION", "ALERTE");
        assertThat(mesures.get(2).get("interpretation").asText()).contains("Hypoglycémie");
        assertThat(mesures.get(0).get("unite").asText()).isEqualTo("mmHg");
    }

    @Test
    void droitsDeSaisie() throws Exception {
        // Un autre patient ne lit ni n'ecrit
        appeler(get(base + "/mesures"), bruno).andExpect(status().isForbidden());
        envoyer(post(base + "/mesures"), Map.of("type", "POIDS", "valeur", 60), bruno).andExpect(status().isForbidden());
        // Un medecin lit tout, mais n'ecrit qu'avec autorisation
        appeler(get(base + "/mesures"), medecin).andExpect(status().isOk());
        envoyer(post(base + "/mesures"), Map.of("type", "POIDS", "valeur", 60), medecin).andExpect(status().isForbidden());
        envoyer(post("/api/carnets/autorisations"), Map.of("medecinId", medecin.getId()), aline).andExpect(status().isOk());
        JsonNode m = json(envoyer(post(base + "/mesures"), Map.of("type", "POIDS", "valeur", 60), medecin).andExpect(status().isCreated()));
        assertThat(m.get("saisieParRole").asText()).isEqualTo("MEDECIN");
        // Seul l'auteur supprime une mesure
        appeler(delete("/api/suivi/mesures/" + m.get("id").asText()), aline).andExpect(status().isForbidden());
        appeler(delete("/api/suivi/mesures/" + m.get("id").asText()), medecin).andExpect(status().isNoContent());
        // Les rappels de medicaments sont geres par le patient seulement
        envoyer(post(base + "/rappels"), Map.of("medicament", "Metformine", "heures", List.of("08:00", "20:00")), medecin)
                .andExpect(status().isForbidden());
        JsonNode rappel = json(envoyer(post(base + "/rappels"),
                Map.of("medicament", "Metformine", "dosage", "500 mg", "heures", List.of("20:00", "08:00")), aline)
                .andExpect(status().isCreated()));
        assertThat(rappel.get("heures").get(0).asText()).startsWith("08:00");
    }

    @Test
    void vaccinDeclareParLePatientPuisValideParUnMedecin() throws Exception {
        String id = json(envoyer(post(base + "/vaccinations"),
                Map.of("vaccin", "Fievre jaune", "dateVaccination", "2019-03-10"), aline).andExpect(status().isCreated()))
                .get("id").asText();
        assertThat(json(appeler(get(base + "/vaccinations"), aline)).get(0).get("statut").asText()).isEqualTo("DECLAREE");

        appeler(patch("/api/suivi/vaccinations/" + id + "/valider"), medecin).andExpect(status().isForbidden());
        envoyer(post("/api/carnets/autorisations"), Map.of("medecinId", medecin.getId()), aline).andExpect(status().isOk());
        JsonNode valide = json(appeler(patch("/api/suivi/vaccinations/" + id + "/valider"), medecin).andExpect(status().isOk()));
        assertThat(valide.get("statut").asText()).isEqualTo("VALIDEE");
        assertThat(valide.get("valideParNom").asText()).isEqualTo("Dr Paul Kamga");
        // Un vaccin valide ne peut plus etre supprime par le patient
        appeler(delete("/api/suivi/vaccinations/" + id), aline).andExpect(status().isBadRequest());
    }

    @Test
    void grossesseAvecAgeGestationnelTermeEtContactsOms() throws Exception {
        LocalDate ddr = LocalDate.now().minusWeeks(21).minusDays(3);
        JsonNode g = json(envoyer(post(base + "/grossesses"), Map.of("dateDernieresRegles", ddr.toString()), aline)
                .andExpect(status().isCreated()));
        assertThat(g.get("semainesAmenorrhee").asInt()).isEqualTo(21);
        assertThat(g.get("joursAmenorrhee").asInt()).isEqualTo(3);
        assertThat(g.get("trimestre").asInt()).isEqualTo(2);
        assertThat(g.get("dateTermePrevue").asText()).isEqualTo(ddr.plusDays(280).toString());
        assertThat(g.get("contactsRecommandes")).hasSize(8);
        assertThat(g.get("contactsRecommandes").get(1).get("passe").asBoolean()).isTrue();  // 20 SA
        assertThat(g.get("contactsRecommandes").get(2).get("passe").asBoolean()).isFalse(); // 26 SA

        // Pas deux grossesses en cours ; DDR aberrante refusee
        envoyer(post(base + "/grossesses"), Map.of("dateDernieresRegles", ddr.toString()), aline).andExpect(status().isBadRequest());

        // La visite prenatale est reservee a un medecin autorise
        String id = g.get("id").asText();
        Map<String, Object> visite = Map.of("date", LocalDate.now().toString(), "tensionSystolique", 120, "tensionDiastolique", 75);
        envoyer(post("/api/suivi/grossesses/" + id + "/visites"), visite, aline).andExpect(status().isForbidden());
        envoyer(post("/api/carnets/autorisations"), Map.of("medecinId", medecin.getId()), aline).andExpect(status().isOk());
        JsonNode apres = json(envoyer(post("/api/suivi/grossesses/" + id + "/visites"), visite, medecin).andExpect(status().isOk()));
        assertThat(apres.get("visites").get(0).get("ageGestationnel").asText()).isEqualTo("21 SA + 3 j");
    }

    private List<String> niveaux(JsonNode mesures) {
        return java.util.stream.StreamSupport.stream(mesures.spliterator(), false).map(m -> m.get("niveau").asText()).toList();
    }

    private Patient patient(String prenom) {
        Patient p = utilisateurRepository.save(Patient.builder().nom("Test").prenom(prenom).email(prenom + "@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        dossierMedicalRepository.save(DossierMedical.builder().patient(p).build());
        return p;
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

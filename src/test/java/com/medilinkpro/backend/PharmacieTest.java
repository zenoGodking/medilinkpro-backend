package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Pharmacien;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import com.medilinkpro.backend.repository.OrdonnanceRepository;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PharmacieTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired DossierMedicalRepository dossierMedicalRepository;
    @Autowired OrdonnanceRepository ordonnanceRepository;
    @Autowired JwtService jwtService;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Patient aline;
    private Patient bruno;
    private Medecin medecin;
    private Pharmacien pharmaA;
    private Pharmacien pharmaB;
    private UUID ordonnanceId;

    @BeforeEach
    void preparer() throws Exception {
        aline = patient("aline");
        bruno = patient("bruno");
        medecin = utilisateurRepository.save(Medecin.builder().nom("Kamga").prenom("Paul").email("kamga@test.cm").motDePasse("x")
                .numeroOrdre("CM-4521").specialite("Generaliste")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        pharmaA = pharmacien("pharmaa", "Pharmacie du Centre");
        pharmaB = pharmacien("pharmab", "Pharmacie Bastos");

        envoyer(post("/api/carnets/autorisations"), Map.of("medecinId", medecin.getId()), aline).andExpect(status().isOk());
        String consultationId = json(envoyer(post("/api/consultations"),
                Map.of("patientId", aline.getId(), "medecinId", medecin.getId(), "motif", "Angine"), medecin)
                .andExpect(status().isCreated())).get("id").asText();
        ordonnanceId = UUID.fromString(json(envoyer(post("/api/ordonnances"), Map.of("consultationId", consultationId,
                "medicaments", "Amoxicilline 1 g", "posologie", "1 comprime matin et soir, 6 jours"), medecin)
                .andExpect(status().isCreated())).get("id").asText());
    }

    @Test
    void verificationPuisDelivranceUniqueEnPharmacie() throws Exception {
        String qr = "/api/pharmacie/ordonnances/" + ordonnanceId + "/qr";
        appeler(get(qr), bruno).andExpect(status().isForbidden());
        String jeton = json(appeler(get(qr), aline).andExpect(status().isOk())).get("jeton").asText();
        assertThat(json(appeler(get(qr), aline)).get("jeton").asText()).isEqualTo(jeton);

        JsonNode verif = json(appeler(get("/api/pharmacie/verifier/" + jeton), pharmaA).andExpect(status().isOk()));
        assertThat(verif.get("medicaments").asText()).isEqualTo("Amoxicilline 1 g");
        assertThat(verif.get("patientPrenom").asText()).isEqualTo("aline");
        assertThat(verif.get("medecinNumeroOrdre").asText()).isEqualTo("CM-4521");
        assertThat(verif.get("delivree").asBoolean()).isFalse();
        assertThat(verif.get("expiree").asBoolean()).isFalse();

        JsonNode delivree = json(appeler(post("/api/pharmacie/delivrer/" + jeton), pharmaA).andExpect(status().isOk()));
        assertThat(delivree.get("delivree").asBoolean()).isTrue();
        assertThat(delivree.get("delivreePar").asText()).contains("Pharmacie du Centre");

        // Une deuxieme pharmacie (ou la meme) ne peut pas la delivrer a nouveau
        String message = json(appeler(post("/api/pharmacie/delivrer/" + jeton), pharmaB).andExpect(status().isConflict()))
                .get("message").asText();
        assertThat(message).contains("déjà délivrée").contains("Pharmacie du Centre");

        assertThat(json(appeler(get("/api/pharmacie/mes-delivrances"), pharmaA))).hasSize(1);
        assertThat(json(appeler(get("/api/pharmacie/mes-delivrances"), pharmaB))).isEmpty();

        // Le patient le voit dans son journal
        JsonNode journal = json(appeler(get("/api/carnets/journal"), aline));
        assertThat(journal).extracting(a -> a.get("typeAcces").asText()).contains("PHARMACIE");
    }

    @Test
    void faussesOrdonnancesExpirationEtCloisonnement() throws Exception {
        appeler(get("/api/pharmacie/verifier/faux-code-imprime"), pharmaA).andExpect(status().isNotFound());

        String jeton = json(appeler(get("/api/pharmacie/ordonnances/" + ordonnanceId + "/qr"), aline)).get("jeton").asText();
        // Seul un pharmacien verifie ; le pharmacien n'accede pas au carnet
        appeler(get("/api/pharmacie/verifier/" + jeton), medecin).andExpect(status().isForbidden());
        appeler(get("/api/carnets/" + aline.getId()), pharmaA).andExpect(status().isForbidden());
        appeler(get("/api/consultations/patient/" + aline.getId()), pharmaA).andExpect(status().isForbidden());

        // Ordonnance de plus de 3 mois : non delivrable
        // (date_emission n'est pas modifiable par l'application : on vieillit l'ordonnance en SQL)
        jdbcTemplate.update("UPDATE ordonnances SET date_emission = DATEADD('MONTH', -4, date_emission) WHERE id = ?", ordonnanceId);
        assertThat(json(appeler(get("/api/pharmacie/verifier/" + jeton), pharmaA)).get("expiree").asBoolean()).isTrue();
        appeler(post("/api/pharmacie/delivrer/" + jeton), pharmaA).andExpect(status().isBadRequest());
    }

    @Test
    void inscriptionPharmacienEnAttenteDeValidation() throws Exception {
        JsonNode r = json(mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nom", "Ngo", "prenom", "Marie", "email", "marie@test.cm",
                        "motDePasse", "secret123", "role", "PHARMACIEN", "nomPharmacie", "Pharmacie Mvog-Mbi",
                        "numeroAgrement", "PH-778")))).andExpect(status().isCreated()));
        assertThat(r.has("token")).isFalse();
        assertThat(r.get("role").asText()).isEqualTo("PHARMACIEN");
    }

    private Patient patient(String prenom) {
        Patient p = utilisateurRepository.save(Patient.builder().nom("Mballa").prenom(prenom).email(prenom + "@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        dossierMedicalRepository.save(DossierMedical.builder().patient(p).build());
        return p;
    }

    private Pharmacien pharmacien(String nom, String pharmacie) {
        return utilisateurRepository.save(Pharmacien.builder().nom(nom).prenom("Ph").email(nom + "@test.cm").motDePasse("x")
                .nomPharmacie(pharmacie).role(Role.PHARMACIEN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
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

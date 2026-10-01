package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.Directeur;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.InfirmierRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Photo obligatoire des infirmieres, profil presente au patient et adhesion a un etablissement. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class InfirmierProfilTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired InfirmierRepository infirmierRepository;
    @Autowired JwtService jwtService;

    private Directeur directeur;
    private Directeur autreDirecteur;
    private Infirmier infirmiere;
    private Patient patient;
    private Patient autrePatient;

    private final MockMultipartFile photo = new MockMultipartFile("photo", "visage.jpg", "image/jpeg", new byte[]{9, 8, 7});

    @BeforeEach
    void preparer() {
        directeur = utilisateurRepository.save(Directeur.builder().nom("Dir").prenom("A").email("dir@test.cm").motDePasse("x")
                .role(Role.DIRECTEUR).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        autreDirecteur = utilisateurRepository.save(Directeur.builder().nom("Dir").prenom("B").email("dirb@test.cm").motDePasse("x")
                .role(Role.DIRECTEUR).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        // Compte cree avant l'obligation de photo
        infirmiere = utilisateurRepository.save(Infirmier.builder().nom("Ngo").prenom("Carine").email("carine@test.cm")
                .motDePasse("x").telephone("+237699000000").role(Role.INFIRMIER).statutCompte(StatutCompte.APPROUVE).actif(true)
                .latitude(3.848).longitude(11.502).datePosition(LocalDateTime.now()).build());
        patient = utilisateurRepository.save(Patient.builder().nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        autrePatient = utilisateurRepository.save(Patient.builder().nom("Etoa").prenom("Bruno").email("bruno@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
    }

    @Test
    void lInscriptionDUneInfirmiereExigeUnePhoto() throws Exception {
        Map<String, Object> donnees = Map.of("nom", "Abena", "prenom", "Rose", "email", "rose@test.cm",
                "motDePasse", "secret123", "role", "INFIRMIER");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(donnees))).andExpect(status().isBadRequest());

        MockMultipartFile json = new MockMultipartFile("donnees", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(donnees));
        mockMvc.perform(multipart("/api/auth/register").file(json).file(photo)).andExpect(status().isCreated());
        Infirmier rose = (Infirmier) utilisateurRepository.findByEmail("rose@test.cm").orElseThrow();
        assertThat(rose.getPhotoProfilChemin()).startsWith("photos-infirmiers/");
    }

    @Test
    void lePatientVoitLeProfilEtLaPhotoDeLInfirmiereQuiVient() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("adresse", "Bastos", "latitude", 3.848, "longitude", 11.5021));
        String alerteId = json(appeler(post("/api/alertes/patients/" + patient.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body), patient).andExpect(status().isCreated())).get("id").asText();

        // Sans photo, l'infirmiere ne peut pas prendre la mission
        appeler(patch("/api/alertes/" + alerteId + "/repondre").param("infirmierId", infirmiere.getId().toString()), infirmiere)
                .andExpect(status().isBadRequest());
        // Avant toute prise en charge, le patient ne voit pas son profil
        appeler(get("/api/infirmiers/" + infirmiere.getId() + "/profil"), patient).andExpect(status().isForbidden());

        appeler(multipart(HttpMethod.PUT, "/api/infirmiers/moi/photo").file(photo), infirmiere).andExpect(status().isOk());
        appeler(patch("/api/alertes/" + alerteId + "/repondre").param("infirmierId", infirmiere.getId().toString()), infirmiere)
                .andExpect(status().isOk());

        JsonNode profil = json(appeler(get("/api/infirmiers/" + infirmiere.getId() + "/profil"), patient).andExpect(status().isOk()));
        assertThat(profil.get("prenom").asText()).isEqualTo("Carine");
        assertThat(profil.get("photoDisponible").asBoolean()).isTrue();
        assertThat(profil.has("statutCompte")).isFalse();
        byte[] image = appeler(get("/api/infirmiers/" + infirmiere.getId() + "/photo"), patient)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(image).containsExactly(9, 8, 7);

        appeler(get("/api/infirmiers/" + infirmiere.getId() + "/photo"), autrePatient).andExpect(status().isForbidden());
    }

    @Test
    void lInfirmiereDemandeARejoindreUnEtablissementEtLeDirecteurValide() throws Exception {
        String etab = json(appeler(post("/api/etablissements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Clinique A\"}"), directeur)).get("id").asText();

        String demandeId = json(appeler(post("/api/infirmiers/moi/demander-integration/" + etab), infirmiere)
                .andExpect(status().isCreated())).get("id").asText();
        appeler(post("/api/infirmiers/moi/demander-integration/" + etab), infirmiere).andExpect(status().isConflict());
        appeler(post("/api/infirmiers/moi/demander-integration/" + etab), patient).andExpect(status().isForbidden());

        JsonNode enAttente = json(appeler(get("/api/demandes-integration/en-attente"), directeur));
        assertThat(enAttente).hasSize(1);
        assertThat(enAttente.get(0).get("typeProfessionnel").asText()).isEqualTo("INFIRMIER");
        assertThat(enAttente.get(0).get("professionnelPrenom").asText()).isEqualTo("Carine");
        assertThat(json(appeler(get("/api/demandes-integration/en-attente"), autreDirecteur))).isEmpty();

        String accepter = "{\"accepter\":true}";
        appeler(patch("/api/demandes-integration/" + demandeId + "/repondre").contentType(MediaType.APPLICATION_JSON)
                .content(accepter), infirmiere).andExpect(status().isForbidden());
        appeler(patch("/api/demandes-integration/" + demandeId + "/repondre").contentType(MediaType.APPLICATION_JSON)
                .content(accepter), autreDirecteur).andExpect(status().isForbidden());
        appeler(patch("/api/demandes-integration/" + demandeId + "/repondre").contentType(MediaType.APPLICATION_JSON)
                .content(accepter), directeur).andExpect(status().isOk());

        assertThat(json(appeler(get("/api/infirmiers/moi"), infirmiere)).get("etablissementNom").asText()).isEqualTo("Clinique A");
        assertThat(json(appeler(get("/api/infirmiers/moi/demandes-integration"), infirmiere)).get(0).get("statut").asText())
                .isEqualTo("ACCEPTEE");
        assertThat(json(appeler(get("/api/etablissements/" + etab + "/infirmiers"), directeur))).hasSize(1);
        appeler(get("/api/etablissements/" + etab + "/infirmiers"), autreDirecteur).andExpect(status().isForbidden());
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, Utilisateur u) throws Exception {
        return mockMvc.perform(requete.header("Authorization",
                "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow())));
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }
}

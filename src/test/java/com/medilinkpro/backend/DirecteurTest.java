package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.Directeur;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.GroupeSanguin;
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

import java.time.LocalDateTime;
import java.util.Map;

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
class DirecteurTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired DossierMedicalRepository dossierMedicalRepository;
    @Autowired JwtService jwtService;

    private Directeur dirA;
    private Directeur dirB;
    private Medecin medecin;
    private Medecin autreMedecin;
    private Patient patient;
    private Utilisateur admin;

    @BeforeEach
    void preparer() {
        dirA = utilisateurRepository.save(Directeur.builder().nom("A").prenom("Dir").email("dira@test.cm").motDePasse("x")
                .role(Role.DIRECTEUR).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        dirB = utilisateurRepository.save(Directeur.builder().nom("B").prenom("Dir").email("dirb@test.cm").motDePasse("x")
                .role(Role.DIRECTEUR).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        medecin = utilisateurRepository.save(Medecin.builder().nom("Kamga").prenom("Paul").email("kamga@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        autreMedecin = utilisateurRepository.save(Medecin.builder().nom("Etoa").prenom("Paul").email("etoa@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        patient = utilisateurRepository.save(Patient.builder().nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .telephone("+237600000001").groupeSanguin(GroupeSanguin.O_POSITIF).allergies("Penicilline")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        dossierMedicalRepository.save(DossierMedical.builder().patient(patient).build());
        admin = utilisateurRepository.findByEmail("womeloic@gmail.com").orElseThrow();
    }

    @Test
    void unDirecteurNeGereQueSesEtablissements() throws Exception {
        JsonNode e1 = json(creerEtablissement("Clinique A", dirA).andExpect(status().isCreated()));
        String id = e1.get("id").asText();
        assertThat(e1.get("directeurId").asText()).isEqualTo(dirA.getId().toString());

        String modif = objectMapper.writeValueAsString(Map.of("nom", "Clinique A renovee"));
        appeler(put("/api/etablissements/" + id).contentType(MediaType.APPLICATION_JSON).content(modif), dirB)
                .andExpect(status().isForbidden());
        appeler(put("/api/etablissements/" + id).contentType(MediaType.APPLICATION_JSON).content(modif), dirA)
                .andExpect(status().isOk());

        String campagne = objectMapper.writeValueAsString(Map.of("titre", "Vaccination", "description", "Rougeole",
                "dateDebut", java.time.LocalDate.now().toString()));
        appeler(post("/api/etablissements/" + id + "/campagnes").contentType(MediaType.APPLICATION_JSON).content(campagne), dirB)
                .andExpect(status().isForbidden());
        appeler(get("/api/etablissements/" + id + "/demandes-integration"), dirB).andExpect(status().isForbidden());
        appeler(post("/api/etablissements/" + id + "/inviter-medecin/" + medecin.getId()), dirB).andExpect(status().isForbidden());

        assertThat(json(appeler(get("/api/directeur/etablissements"), dirA))).hasSize(1);
        assertThat(json(appeler(get("/api/directeur/etablissements"), dirB))).isEmpty();
    }

    @Test
    void lAdminAttribueUnDirecteurAUnEtablissementExistant() throws Exception {
        String id = json(creerEtablissement("Hopital central", admin)).get("id").asText();
        appeler(put("/api/etablissements/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"X\"}"), dirB)
                .andExpect(status().isForbidden());

        appeler(patch("/api/admin/etablissements/" + id + "/directeur").contentType(MediaType.APPLICATION_JSON)
                .content("{\"directeurId\":\"" + dirB.getId() + "\"}"), admin).andExpect(status().isOk());
        appeler(put("/api/etablissements/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"X\"}"), dirB)
                .andExpect(status().isOk());
        // Un non-directeur ne peut pas etre attribue
        appeler(patch("/api/admin/etablissements/" + id + "/directeur").contentType(MediaType.APPLICATION_JSON)
                .content("{\"directeurId\":\"" + medecin.getId() + "\"}"), admin).andExpect(status().isBadRequest());
    }

    @Test
    void seulLeDirecteurDeLEtablissementAccepteUnMedecinEtVoitSesPatients() throws Exception {
        String id = json(creerEtablissement("Clinique A", dirA)).get("id").asText();

        // Un medecin ne peut pas demander au nom d'un autre
        appeler(post("/api/medecins/" + medecin.getId() + "/demander-integration/" + id), autreMedecin)
                .andExpect(status().isForbidden());
        String demandeId = json(appeler(post("/api/medecins/" + medecin.getId() + "/demander-integration/" + id), medecin)
                .andExpect(status().isCreated())).get("id").asText();

        // Le directeur B ne peut pas l'accepter, meme en se faisant passer pour A via actorId
        appeler(patch("/api/demandes-integration/" + demandeId + "/repondre").param("actorId", dirA.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"accepter\":true}"), dirB)
                .andExpect(status().isForbidden());
        appeler(patch("/api/demandes-integration/" + demandeId + "/repondre")
                .contentType(MediaType.APPLICATION_JSON).content("{\"accepter\":true}"), dirA)
                .andExpect(status().isOk());

        // Le patient prend rendez-vous avec ce medecin (sans preciser d'etablissement)
        appeler(post("/api/rendez-vous").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                "patientId", patient.getId(), "medecinId", medecin.getId(),
                "dateHeure", java.time.LocalDate.now().plusWeeks(1).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY)).atTime(9, 0).toString()))), patient)
                .andExpect(status().isCreated());

        JsonNode patientsA = json(appeler(get("/api/directeur/patients"), dirA).andExpect(status().isOk()));
        assertThat(patientsA).hasSize(1);
        JsonNode aline = patientsA.get(0);
        assertThat(aline.get("prenom").asText()).isEqualTo("Aline");
        assertThat(aline.get("nombreRendezVous").asInt()).isEqualTo(1);
        assertThat(aline.get("etablissements").get(0).asText()).isEqualTo("Clinique A");
        // Aucune donnee medicale
        assertThat(aline.has("groupeSanguin")).isFalse();
        assertThat(aline.has("allergies")).isFalse();

        assertThat(json(appeler(get("/api/directeur/patients"), dirB))).isEmpty();
        appeler(get("/api/directeur/patients"), medecin).andExpect(status().isForbidden());
        // Le directeur n'a toujours pas acces au carnet
        appeler(get("/api/carnets/" + patient.getId()), dirA).andExpect(status().isForbidden());
    }

    private ResultActions creerEtablissement(String nom, Utilisateur u) throws Exception {
        return appeler(post("/api/etablissements").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nom", nom, "type", "CLINIQUE", "adresse", "Bastos",
                        "ville", "Yaounde"))), u);
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, Utilisateur u) throws Exception {
        return mockMvc.perform(requete.header("Authorization",
                "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow())));
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }
}

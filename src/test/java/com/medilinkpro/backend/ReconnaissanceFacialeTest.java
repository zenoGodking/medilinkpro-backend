package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "medilinkpro.upload.private-dir=${java.io.tmpdir}/medilinkpro-test-private")
class ReconnaissanceFacialeTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private String tokenPatient;

    @BeforeEach
    void inscrirePatients() throws Exception {
        // Quatre patients : le visage scanne correspondra a "Aline" (distance ~0.2),
        // "Brice" et "Carine" sont plausibles, "Denis" et "Eric" sont trop eloignes.
        tokenPatient = inscrire("Aline", "aline@test.cm", vecteur(0.00));
        inscrire("Brice", "brice@test.cm", vecteur(0.035));
        inscrire("Carine", "carine@test.cm", vecteur(0.04));
        inscrire("Denis", "denis@test.cm", vecteur(0.045));
        inscrire("Eric", "eric@test.cm", vecteur(0.09));
    }

    @Test
    void inscriptionPatientSansPhotoRefusee() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "nom", "X", "prenom", "Y", "email", "sansphoto@test.cm", "motDePasse", "secret123", "role", "PATIENT"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void utilisateurConnecteVoitSeulementLEssentielEtAuPlusTroisCandidats() throws Exception {
        JsonNode resultat = rechercher(tokenPatient, vecteur(0.005));

        assertThat(resultat.get("niveauAcces").asText()).isEqualTo("ESSENTIEL");
        assertThat(resultat.get("avertissement").asText()).contains("Correspondance probable");
        JsonNode candidats = resultat.get("candidats");
        assertThat(candidats).hasSize(3);
        assertThat(candidats.get(0).get("prenom").asText()).isEqualTo("Aline");
        assertThat(candidats.get(0).get("niveauConfiance").asText()).isEqualTo("ELEVEE");
        assertThat(candidats.get(0).get("scoreConfiance").asInt()).isGreaterThan(candidats.get(1).get("scoreConfiance").asInt());
        assertThat(candidats.get(0).get("groupeSanguin").asText()).isEqualTo("O_POSITIF");
        assertThat(candidats.get(0).get("contactUrgenceTelephone").asText()).isEqualTo("+237600000000");
        assertThat(candidats.get(0).get("photoReference").asText()).startsWith("data:image/jpeg;base64,");
        // Pas d'identite complete pour un simple utilisateur
        assertThat(candidats.get(0).has("nom")).isFalse();
        assertThat(candidats.get(0).has("dateNaissance")).isFalse();
        // "Eric" est au-dela du seuil : jamais expose
        candidats.forEach(c -> assertThat(c.get("prenom").asText()).isNotEqualTo("Eric"));
    }

    @Test
    void aucunCandidatSiVisageInconnu() throws Exception {
        JsonNode resultat = rechercher(tokenPatient, vecteur(-0.08));
        assertThat(resultat.get("candidats")).isEmpty();
    }

    @Test
    void personnelSanteVoitLeCarnetCompletEnLectureSeule() throws Exception {
        String tokenInfirmier = professionnel(StatutCompte.APPROUVE);
        JsonNode resultat = rechercher(tokenInfirmier, vecteur(0.0));
        assertThat(resultat.get("niveauAcces").asText()).isEqualTo("COMPLET");
        JsonNode aline = resultat.get("candidats").get(0);
        assertThat(aline.get("nom").asText()).isEqualTo("Test");

        String carnet = mockMvc.perform(get("/api/reconnaissance-faciale/patients/" + aline.get("patientId").asText() + "/carnet")
                        .header("Authorization", "Bearer " + tokenInfirmier))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode carnetJson = objectMapper.readTree(carnet);
        assertThat(carnetJson.get("lectureSeule").asBoolean()).isTrue();
        assertThat(carnetJson.get("patient").get("allergies").asText()).isEqualTo("Penicilline");
    }

    @Test
    void carnetCompletInterditAuxNonSoignants() throws Exception {
        JsonNode aline = rechercher(tokenPatient, vecteur(0.0)).get("candidats").get(0);
        mockMvc.perform(get("/api/reconnaissance-faciale/patients/" + aline.get("patientId").asText() + "/carnet")
                        .header("Authorization", "Bearer " + tokenPatient))
                .andExpect(status().isForbidden());

        String tokenNonValide = professionnel(StatutCompte.EN_ATTENTE);
        mockMvc.perform(get("/api/reconnaissance-faciale/patients/" + aline.get("patientId").asText() + "/carnet")
                        .header("Authorization", "Bearer " + tokenNonValide))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechercheInterditeSansConnexion() throws Exception {
        mockMvc.perform(post("/api/reconnaissance-faciale/recherche")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("descripteur", vecteur(0.0)))))
                .andExpect(status().is4xxClientError());
    }

    /** Vecteur de 128 valeurs : chaque composante decalee de "decalage" => distance = |decalage| * sqrt(128). */
    private static List<Double> vecteur(double decalage) {
        Double[] v = new Double[128];
        for (int i = 0; i < v.length; i++) {
            v[i] = ((i % 7) - 3) * 0.02 + decalage;
        }
        return new ArrayList<>(Arrays.asList(v));
    }

    private String inscrire(String prenom, String email, List<Double> descripteur) throws Exception {
        Map<String, Object> donnees = Map.of(
                "nom", "Test", "prenom", prenom, "email", email, "motDePasse", "secret123", "role", "PATIENT",
                "groupeSanguin", "O_POSITIF", "allergies", "Penicilline",
                "contactUrgenceNom", "Proche", "contactUrgenceTelephone", "+237600000000");
        String reponse = mockMvc.perform(multipart("/api/auth/register")
                        .file(new MockMultipartFile("donnees", "", "application/json", objectMapper.writeValueAsBytes(donnees)))
                        .file(new MockMultipartFile("photo", "visage.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .file(new MockMultipartFile("descripteur", "", "application/json", objectMapper.writeValueAsBytes(descripteur))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("token").asText();
    }

    private String professionnel(StatutCompte statut) {
        Utilisateur infirmier = utilisateurRepository.save(Infirmier.builder()
                .nom("Soignant").prenom("Sam").email("infirmier-" + statut + "@test.cm")
                .motDePasse(passwordEncoder.encode("secret123")).role(Role.INFIRMIER)
                .statutCompte(statut).actif(true).build());
        return jwtService.generateToken(utilisateurRepository.findById(infirmier.getId()).orElseThrow());
    }

    private JsonNode rechercher(String token, List<Double> descripteur) throws Exception {
        String reponse = mockMvc.perform(post("/api/reconnaissance-faciale/recherche")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("descripteur", descripteur))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse);
    }
}

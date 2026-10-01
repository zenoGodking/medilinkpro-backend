package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.config.MigrationChiffrement;
import com.medilinkpro.backend.config.VerificationConfigurationProduction;
import com.medilinkpro.backend.entity.Directeur;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.PatientRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import com.medilinkpro.backend.securite.ServiceChiffrement;
import com.medilinkpro.backend.service.sms.FournisseurSms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SecuriteTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MigrationChiffrement migrationChiffrement;
    @MockBean FournisseurSms fournisseurSms;

    private Medecin drKamga;
    private Medecin drEtoa;
    private Directeur directeur;
    private Utilisateur admin;

    @BeforeEach
    void preparer() {
        drKamga = utilisateurRepository.save(Medecin.builder().nom("Kamga").prenom("Paul").email("kamga@test.cm")
                .motDePasse(passwordEncoder.encode("BonMotDePasse1")).telephone("+237690000001").numeroOrdre("ONMC-1")
                .tarif(new java.math.BigDecimal("10000")).role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        drEtoa = utilisateurRepository.save(Medecin.builder().nom("Etoa").prenom("Jean").email("etoa@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        directeur = utilisateurRepository.save(Directeur.builder().nom("Dir").prenom("A").email("dir@test.cm").motDePasse("x")
                .role(Role.DIRECTEUR).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        admin = utilisateurRepository.findByEmail("womeloic@gmail.com").orElseThrow();
    }

    @Test
    void unMedecinNeModifieQueSaPropreFicheEtSeulLAdminSupprime() throws Exception {
        appeler(put("/api/medecins/" + drEtoa.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"tarif\":1}"), drKamga)
                .andExpect(status().isForbidden());
        appeler(put("/api/medecins/" + drKamga.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"tarif\":1}"), directeur)
                .andExpect(status().isForbidden());
        appeler(put("/api/medecins/" + drKamga.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"verifie\":true}"), drKamga)
                .andExpect(status().isForbidden());
        appeler(put("/api/medecins/" + drKamga.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroOrdre\":\"FAUX-99\"}"), drKamga).andExpect(status().isBadRequest());
        // Le formulaire de profil renvoie le numero d'ordre inchange : accepte
        appeler(put("/api/medecins/" + drKamga.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tarif\":15000,\"numeroOrdre\":\"ONMC-1\"}"), drKamga).andExpect(status().isOk());
        appeler(put("/api/medecins/" + drKamga.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"verifie\":true}"), admin)
                .andExpect(status().isOk());

        appeler(delete("/api/medecins/" + drEtoa.getId()), drKamga).andExpect(status().isForbidden());
        appeler(delete("/api/medecins/" + drEtoa.getId()), directeur).andExpect(status().isForbidden());
        appeler(delete("/api/medecins/" + drEtoa.getId()), admin).andExpect(status().isNoContent());
    }

    @Test
    void lesDonneesDeSanteSontChiffreesEnBaseEtLesAnciennesDonneesMigrees() {
        Patient p = patientRepository.save(Patient.builder().nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .allergies("Penicilline").antecedents("Asthme").role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());

        String brut = jdbcTemplate.queryForObject("SELECT allergies FROM patients WHERE id = ?", String.class, p.getId());
        assertThat(brut).startsWith(ServiceChiffrement.PREFIXE_TEXTE).doesNotContain("Penicilline");
        assertThat(patientRepository.findById(p.getId()).orElseThrow().getAllergies()).isEqualTo("Penicilline");

        // Donnee anterieure au chiffrement : relue telle quelle, puis chiffree par la migration
        jdbcTemplate.update("UPDATE patients SET antecedents = 'Diabete' WHERE id = ?", p.getId());
        assertThat(patientRepository.findById(p.getId()).orElseThrow().getAntecedents()).isEqualTo("Diabete");
        migrationChiffrement.run(null);
        assertThat(jdbcTemplate.queryForObject("SELECT antecedents FROM patients WHERE id = ?", String.class, p.getId()))
                .startsWith(ServiceChiffrement.PREFIXE_TEXTE);
        assertThat(patientRepository.findById(p.getId()).orElseThrow().getAntecedents()).isEqualTo("Diabete");
    }

    @Test
    void laConnexionEstBloqueeApresCinqEchecs() throws Exception {
        for (int i = 0; i < 5; i++) {
            connexion("kamga@test.cm", "mauvais").andExpect(status().isUnauthorized());
        }
        connexion("kamga@test.cm", "BonMotDePasse1").andExpect(status().isTooManyRequests());
        // Un autre compte n'est pas penalise
        connexion("womeloic@gmail.com", "Hope123").andExpect(status().isOk());
    }

    @Test
    void reinitialisationDuMotDePasseParCodeSms() throws Exception {
        String reponse = mockMvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"kamga@test.cm\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // Meme reponse pour un compte inexistant (pas d'enumeration des comptes)
        String inconnu = mockMvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"personne@test.cm\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(inconnu).isEqualTo(reponse);

        ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
        verify(fournisseurSms).envoyer(eq("+237690000001"), sms.capture());
        Matcher m = Pattern.compile("(\\d{6})").matcher(sms.getValue());
        assertThat(m.find()).isTrue();
        String code = m.group(1);
        // Le code n'est jamais archive en clair
        assertThat(jdbcTemplate.queryForList("SELECT message FROM notifications_sms", String.class))
                .allSatisfy(message -> assertThat(message).doesNotContain(code));

        String mauvais = code.equals("000000") ? "111111" : "000000";
        reinitialiser(mauvais, "NouveauSecret42").andExpect(status().isBadRequest());
        reinitialiser(code, "court").andExpect(status().isBadRequest());
        reinitialiser(code, "NouveauSecret42").andExpect(status().isOk());
        reinitialiser(code, "EncoreUnAutre42").andExpect(status().isBadRequest()); // usage unique

        connexion("kamga@test.cm", "BonMotDePasse1").andExpect(status().isUnauthorized());
        connexion("kamga@test.cm", "NouveauSecret42").andExpect(status().isOk());
    }

    @Test
    void cinqCodesFauxInvalidentLeCode() throws Exception {
        mockMvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"kamga@test.cm\"}")).andExpect(status().isOk());
        ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
        verify(fournisseurSms).envoyer(anyString(), sms.capture());
        Matcher m = Pattern.compile("(\\d{6})").matcher(sms.getValue());
        assertThat(m.find()).isTrue();
        String code = m.group(1);
        String mauvais = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            reinitialiser(mauvais, "NouveauSecret42").andExpect(status().isBadRequest());
        }
        reinitialiser(code, "NouveauSecret42").andExpect(status().isBadRequest());
    }

    @Test
    void lAdminGenereUnNouveauMotDePassePourUnUtilisateurQuiLaPerdu() throws Exception {
        for (int i = 0; i < 5; i++) {
            connexion("kamga@test.cm", "oublie").andExpect(status().isUnauthorized());
        }
        appeler(post("/api/admin/utilisateurs/" + drKamga.getId() + "/nouveau-mot-de-passe"), drEtoa).andExpect(status().isForbidden());
        appeler(post("/api/admin/utilisateurs/" + admin.getId() + "/nouveau-mot-de-passe"), admin).andExpect(status().isBadRequest());

        String nouveau = objectMapper.readTree(appeler(post("/api/admin/utilisateurs/" + drKamga.getId() + "/nouveau-mot-de-passe"), admin)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("nouveauMotDePasse").asText();
        assertThat(nouveau).matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}");

        connexion("kamga@test.cm", "BonMotDePasse1").andExpect(status().isUnauthorized());
        // Le blocage apres echecs est leve : le nouveau mot de passe fonctionne tout de suite
        connexion("kamga@test.cm", nouveau).andExpect(status().isOk());
    }

    @Test
    void seulesLesOriginesConfigureesPeuventAppelerLAPI() throws Exception {
        mockMvc.perform(get("/api/campagnes/actives").header("Origin", "https://site-malveillant.example"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/campagnes/actives").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk());
    }

    @Test
    void leServeurRefuseDeDemarrerEnProductionAvecDesSecretsFaibles() {
        VerificationConfigurationProduction verification = new VerificationConfigurationProduction(new MockEnvironment());
        ReflectionTestUtils.setField(verification, "jwtSecret", "NDgyZDcwYjQ3YTRmMzM5ZjU0MTk4ZTU0YjQ0YTQ4ODhlMzEzYjc4ZjY0YzQ4NzU5ZjA0ZTQ4ODg1ZjQ4ZTQ4");
        ReflectionTestUtils.setField(verification, "adminEmail", "admin@medilinkpro.cm");
        ReflectionTestUtils.setField(verification, "adminMotDePasse", "Hope123");
        ReflectionTestUtils.setField(verification, "origines", "*");
        ReflectionTestUtils.setField(verification, "cleChiffrement", "");
        assertThatThrownBy(verification::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET").hasMessageContaining("SUPER_ADMIN_PASSWORD")
                .hasMessageContaining("CORS_ORIGINES").hasMessageContaining("CLE_CHIFFREMENT");

        ReflectionTestUtils.setField(verification, "jwtSecret", java.util.Base64.getEncoder().encodeToString(new byte[48]).replace('A', 'B'));
        ReflectionTestUtils.setField(verification, "adminMotDePasse", "Un-Vrai-Secret-2026");
        ReflectionTestUtils.setField(verification, "origines", "https://medilinkpro.vercel.app");
        ReflectionTestUtils.setField(verification, "cleChiffrement", java.util.Base64.getEncoder().encodeToString(new byte[32]));
        assertThatCode(verification::afterPropertiesSet).doesNotThrowAnyException();
    }

    private ResultActions connexion(String email, String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "motDePasse", motDePasse))));
    }

    private ResultActions reinitialiser(String code, String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/reinitialiser-mot-de-passe").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "kamga@test.cm", "code", code, "nouveauMotDePasse", motDePasse))));
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, Utilisateur u) throws Exception {
        return mockMvc.perform(requete.header("Authorization",
                "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow())));
    }
}

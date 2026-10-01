package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.config.SuppressionSecretairesMigration;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.NotificationSms;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import com.medilinkpro.backend.repository.NotificationSmsRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CarnetAccesTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired DossierMedicalRepository dossierMedicalRepository;
    @Autowired NotificationSmsRepository notificationSmsRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired SuppressionSecretairesMigration migration;

    private Patient aline;
    private Patient bruno;
    private Medecin drKamga;
    private Medecin drEtoa;
    private Utilisateur admin;

    @BeforeEach
    void preparer() {
        aline = patient("aline", "+237677000001");
        bruno = patient("bruno", null);
        drKamga = medecin("kamga");
        drEtoa = medecin("etoa");
        admin = utilisateurRepository.findByEmail("womeloic@gmail.com").orElseThrow();
    }

    // ---------------------------------------------------------------- Lecture

    @Test
    void unPatientNeVoitQueSonPropreCarnet() throws Exception {
        appeler(get("/api/carnets/" + aline.getId()), aline).andExpect(status().isOk());
        appeler(get("/api/carnets/" + bruno.getId()), aline).andExpect(status().isForbidden());
        appeler(get("/api/dossiers-medicaux/patient/" + bruno.getId()), aline).andExpect(status().isForbidden());
        appeler(get("/api/consultations/patient/" + bruno.getId()), aline).andExpect(status().isForbidden());
        appeler(get("/api/ordonnances/patient/" + bruno.getId()), aline).andExpect(status().isForbidden());
        appeler(get("/api/patients/" + bruno.getId()), aline).andExpect(status().isForbidden());
        appeler(get("/api/rendez-vous/patient/" + bruno.getId()), aline).andExpect(status().isForbidden());
        appeler(get("/api/patients"), aline).andExpect(status().isForbidden());
        // ni modifier, ni supprimer le compte d'un autre
        appeler(put("/api/patients/" + bruno.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"allergies\":\"aucune\"}"), aline).andExpect(status().isForbidden());
        appeler(delete("/api/patients/" + bruno.getId()), aline).andExpect(status().isForbidden());
    }

    @Test
    void toutMedecinLitTousLesCarnetsMaisNEcritPasSansAutorisation() throws Exception {
        JsonNode carnet = json(appeler(get("/api/carnets/" + aline.getId()), drKamga).andExpect(status().isOk()));
        assertThat(carnet.get("ecritureAutorisee").asBoolean()).isFalse();
        appeler(get("/api/patients"), drKamga).andExpect(status().isOk());

        creerConsultation(drKamga, aline).andExpect(status().isForbidden());
        appeler(put("/api/patients/" + aline.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"allergies\":\"Penicilline\"}"), drKamga).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- Ecriture

    @Test
    void lePatientAutoriseUnMedecinPuisRetireLAutorisation() throws Exception {
        appeler(post("/api/carnets/autorisations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"medecinId\":\"" + drKamga.getId() + "\"}"), aline).andExpect(status().isOk());

        JsonNode carnet = json(appeler(get("/api/carnets/" + aline.getId()), drKamga));
        assertThat(carnet.get("ecritureAutorisee").asBoolean()).isTrue();
        assertThat(carnet.get("motifEcriture").asText()).isEqualTo("AUTORISATION_PATIENT");

        // Le medecin auteur est force a l'utilisateur connecte, meme si la requete pretend le contraire
        JsonNode consultation = json(creerConsultation(drKamga, aline, drEtoa.getId()).andExpect(status().isCreated()));
        assertThat(consultation.get("medecinId").asText()).isEqualTo(drKamga.getId().toString());

        // Un medecin autorise ne modifie que les donnees medicales, pas les coordonnees
        appeler(put("/api/patients/" + aline.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"allergies\":\"Penicilline\",\"telephone\":\"000\"}"), drKamga).andExpect(status().isOk());
        JsonNode patient = json(appeler(get("/api/patients/" + aline.getId()), aline));
        assertThat(patient.get("allergies").asText()).isEqualTo("Penicilline");
        assertThat(patient.get("telephone").asText()).isNotEqualTo("000");

        // Le Dr Etoa n'a rien recu : toujours interdit
        creerConsultation(drEtoa, aline).andExpect(status().isForbidden());

        appeler(delete("/api/carnets/autorisations/" + drKamga.getId()), aline).andExpect(status().isNoContent());
        // Reste ancien patient grace a la consultation deja realisee
        assertThat(json(appeler(get("/api/carnets/" + aline.getId()), drKamga)).get("motifEcriture").asText())
                .isEqualTo("ANCIEN_PATIENT");
    }

    @Test
    void unRendezVousPrisParLePatientFaitDuMedecinSonMedecinTraitant() throws Exception {
        Map<String, Object> rdv = Map.of("patientId", aline.getId(), "medecinId", drEtoa.getId(),
                "dateHeure", java.time.LocalDate.now().plusWeeks(1).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY)).atTime(9, 0).toString());

        // Un medecin ne peut pas s'attribuer un patient en prenant rendez-vous a sa place
        appeler(post("/api/rendez-vous").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rdv)), drEtoa).andExpect(status().isForbidden());

        String rdvId = json(appeler(post("/api/rendez-vous").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rdv)), aline).andExpect(status().isCreated())).get("id").asText();
        // Simple demande : le medecin n'a pas encore de droit d'ecriture
        assertThat(json(appeler(get("/api/carnets/" + aline.getId()), drEtoa)).get("ecritureAutorisee").asBoolean()).isFalse();
        appeler(patch("/api/rendez-vous/" + rdvId + "/accepter"), drKamga).andExpect(status().isForbidden());
        appeler(patch("/api/rendez-vous/" + rdvId + "/accepter"), drEtoa).andExpect(status().isOk());
        assertThat(json(appeler(get("/api/carnets/" + aline.getId()), drEtoa)).get("motifEcriture").asText())
                .isEqualTo("ANCIEN_PATIENT");
        creerConsultation(drEtoa, aline).andExpect(status().isCreated());

        JsonNode accessibles = json(appeler(get("/api/carnets/ecriture-autorisee"), drEtoa));
        assertThat(accessibles).hasSize(1);
        assertThat(accessibles.get(0).get("id").asText()).isEqualTo(aline.getId().toString());
    }

    // ---------------------------------------------------------------- Journal

    @Test
    void lePatientVoitQuiAConsulteEtModifieSonCarnet() throws Exception {
        // Plusieurs lectures rapprochees par le meme medecin = une seule ligne
        appeler(get("/api/carnets/" + aline.getId()), drKamga).andExpect(status().isOk());
        appeler(get("/api/consultations/patient/" + aline.getId()), drKamga).andExpect(status().isOk());
        appeler(post("/api/carnets/autorisations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"medecinId\":\"" + drKamga.getId() + "\"}"), aline).andExpect(status().isOk());
        creerConsultation(drKamga, aline).andExpect(status().isCreated());
        // Ses propres lectures ne sont pas journalisees
        appeler(get("/api/carnets/" + aline.getId()), aline).andExpect(status().isOk());

        JsonNode journal = json(appeler(get("/api/carnets/journal"), aline).andExpect(status().isOk()));
        assertThat(journal).hasSize(2);
        assertThat(journal.get(0).get("typeAcces").asText()).isEqualTo("ECRITURE");
        assertThat(journal.get(1).get("typeAcces").asText()).isEqualTo("LECTURE");
        assertThat(journal.get(1).get("nom").asText()).isEqualTo("Test kamga");
        assertThat(journal.get(1).get("role").asText()).isEqualTo("MEDECIN");

        // Le journal de Bruno est vide, et un medecin n'a pas de journal patient
        assertThat(json(appeler(get("/api/carnets/journal"), bruno))).isEmpty();
        appeler(get("/api/carnets/journal"), drKamga).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- Carte d'urgence (QR)

    @Test
    void carteDUrgenceScanneeParUnUtilisateurConnecte() throws Exception {
        String jeton = json(appeler(get("/api/carte-urgence/moi"), aline).andExpect(status().isOk())).get("jeton").asText();
        assertThat(jeton).hasSizeGreaterThanOrEqualTo(32);
        // Stable tant qu'on ne regenere pas
        assertThat(json(appeler(get("/api/carte-urgence/moi"), aline)).get("jeton").asText()).isEqualTo(jeton);

        // Sans connexion : refuse
        mockMvc.perform(get("/api/carte-urgence/" + jeton)).andExpect(status().is4xxClientError());

        // Un autre patient voit l'essentiel, sans identite complete
        JsonNode vueBruno = json(appeler(get("/api/carte-urgence/" + jeton), bruno).andExpect(status().isOk()));
        assertThat(vueBruno.get("prenom").asText()).isEqualTo("Aline");
        assertThat(vueBruno.get("contactUrgenceTelephone").asText()).isEqualTo("+237677000001");
        assertThat(vueBruno.has("nom")).isFalse();
        assertThat(vueBruno.has("patientId")).isFalse();

        // Un medecin voit l'identite et peut ouvrir le carnet d'urgence
        JsonNode vueMedecin = json(appeler(get("/api/carte-urgence/" + jeton), drKamga));
        assertThat(vueMedecin.get("patientId").asText()).isEqualTo(aline.getId().toString());

        // Le scan apparait dans le journal d'Aline
        JsonNode journal = json(appeler(get("/api/carnets/journal"), aline));
        assertThat(journal).extracting(a -> a.get("typeAcces").asText()).contains("CARTE_URGENCE");

        // Carte perdue : nouveau code, l'ancien ne mene plus a rien
        String nouveau = json(appeler(post("/api/carte-urgence/moi/regenerer"), aline)).get("jeton").asText();
        assertThat(nouveau).isNotEqualTo(jeton);
        appeler(get("/api/carte-urgence/" + jeton), drKamga).andExpect(status().isNotFound());
        appeler(get("/api/carte-urgence/" + nouveau), drKamga).andExpect(status().isOk());

        // Un medecin n'a pas de carte a gerer
        appeler(get("/api/carte-urgence/moi"), drKamga).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- Deces

    @Test
    void toutMedecinPeutDeclarerUnDecesEtLeProcheEstInforme() throws Exception {
        String tokenAline = token(aline);

        JsonNode reponse = json(appeler(post("/api/patients/" + aline.getId() + "/deces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDeces\":\"" + LocalDate.now() + "\",\"circonstances\":\"Accident de la route\"}"), drKamga)
                .andExpect(status().isOk()));
        assertThat(reponse.get("procheTelephone").asText()).isEqualTo("+237677000001");
        assertThat(reponse.get("statutNotification").asText()).isEqualTo("NON_ENVOYE_AUCUN_FOURNISSEUR");

        NotificationSms sms = notificationSmsRepository.findByPatientId(aline.getId()).get(0);
        assertThat(sms.getDestinataire()).isEqualTo("+237677000001");
        assertThat(sms.getMessage()).contains("Aline", "Dr Test");

        // Compte desactive : plus de connexion, et le token deja emis est refuse
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aline@test.cm\",\"motDePasse\":\"secret123\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/carnets/" + aline.getId()).header("Authorization", "Bearer " + tokenAline))
                .andExpect(status().is4xxClientError());

        // Carnet clos, et pas de double declaration
        appeler(post("/api/carnets/autorisations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"medecinId\":\"" + drKamga.getId() + "\"}"), bruno).andExpect(status().isOk());
        assertThat(json(appeler(get("/api/carnets/" + aline.getId()), drKamga)).get("ecritureAutorisee").asBoolean()).isFalse();
        appeler(post("/api/patients/" + aline.getId() + "/deces").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDeces\":\"" + LocalDate.now() + "\"}"), drEtoa).andExpect(status().isBadRequest());

        // Seul l'admin annule une declaration erronee
        appeler(delete("/api/patients/" + aline.getId() + "/deces"), drKamga).andExpect(status().isForbidden());
        appeler(delete("/api/patients/" + aline.getId() + "/deces"), admin).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aline@test.cm\",\"motDePasse\":\"secret123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unPatientNePeutPasDeclarerUnDeces() throws Exception {
        appeler(post("/api/patients/" + bruno.getId() + "/deces").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDeces\":\"" + LocalDate.now() + "\"}"), aline).andExpect(status().isForbidden());
        appeler(post("/api/patients/" + bruno.getId() + "/deces").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDeces\":\"" + LocalDate.now().plusDays(2) + "\"}"), drKamga).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- Secretaire

    @Test
    void leRoleSecretaireNExistePlus() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"S\",\"prenom\":\"S\",\"email\":\"s@test.cm\",\"motDePasse\":\"secret123\",\"role\":\"SECRETAIRE\"}"))
                .andExpect(status().isBadRequest());

        // Un ancien compte secretaire en base est supprime au demarrage
        jdbcTemplate.update("INSERT INTO utilisateurs (id, nom, prenom, email, mot_de_passe, role, actif, statut_compte, date_inscription) "
                + "VALUES (?, 'Old', 'Sec', 'old-sec@test.cm', 'x', 'SECRETAIRE', true, 'APPROUVE', CURRENT_TIMESTAMP)", UUID.randomUUID());
        migration.run(null);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM utilisateurs WHERE role = 'SECRETAIRE'", Integer.class)).isZero();
    }

    // ---------------------------------------------------------------- Outils

    private Patient patient(String prenom, String contactUrgence) {
        Patient p = utilisateurRepository.save(Patient.builder()
                .nom("Test").prenom(Character.toUpperCase(prenom.charAt(0)) + prenom.substring(1))
                .email(prenom + "@test.cm").motDePasse(passwordEncoder.encode("secret123")).telephone("+237600000000")
                .contactUrgenceTelephone(contactUrgence)
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        dossierMedicalRepository.save(DossierMedical.builder().patient(p).build());
        return p;
    }

    private Medecin medecin(String nom) {
        return utilisateurRepository.save(Medecin.builder()
                .nom(nom).prenom("Test").email(nom + "@test.cm").motDePasse("x").specialite("Generaliste")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
    }

    private ResultActions creerConsultation(Medecin medecin, Patient patient) throws Exception {
        return creerConsultation(medecin, patient, medecin.getId());
    }

    private ResultActions creerConsultation(Medecin medecin, Patient patient, UUID medecinIdDeclare) throws Exception {
        return appeler(post("/api/consultations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "patientId", patient.getId(), "medecinId", medecinIdDeclare, "motif", "Controle"))), medecin);
    }

    private ResultActions appeler(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
                                  Utilisateur u) throws Exception {
        return mockMvc.perform(requete.header("Authorization", "Bearer " + token(u)));
    }

    private String token(Utilisateur u) {
        return jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow());
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }
}

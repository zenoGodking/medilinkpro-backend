package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.dto.request.SignalTeleconsultationRequest;
import com.medilinkpro.backend.dto.response.SignalTeleconsultationResponse;
import com.medilinkpro.backend.entity.Directeur;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeConsultation;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import com.medilinkpro.backend.service.TeleconsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Adhesion des medecins, calendrier de disponibilite, documents medicaux et teleconsultation. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NouvellesFonctionnalitesTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired DossierMedicalRepository dossierMedicalRepository;
    @Autowired RendezVousRepository rendezVousRepository;
    @Autowired JwtService jwtService;
    @Autowired TeleconsultationService teleconsultationService;
    @SpyBean SimpMessagingTemplate messagingTemplate;

    private Directeur directeur;
    private Medecin medecin;
    private Medecin autreMedecin;
    private Patient patient;
    private Patient autrePatient;
    private Utilisateur admin;

    /** Lundi de la semaine prochaine : toujours dans le futur, toujours un jour ouvrable. */
    private final LocalDate lundi = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    @BeforeEach
    void preparer() {
        directeur = utilisateurRepository.save(Directeur.builder().nom("Dir").prenom("A").email("dir@test.cm").motDePasse("x")
                .role(Role.DIRECTEUR).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        medecin = utilisateurRepository.save(Medecin.builder().nom("Kamga").prenom("Paul").email("kamga@test.cm").motDePasse("x")
                .numeroOrdre("ONMC-123").role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        autreMedecin = utilisateurRepository.save(Medecin.builder().nom("Etoa").prenom("Jean").email("etoa@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        patient = utilisateurRepository.save(Patient.builder().nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        autrePatient = utilisateurRepository.save(Patient.builder().nom("Ngo").prenom("Bruno").email("bruno@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        dossierMedicalRepository.save(DossierMedical.builder().patient(patient).build());
        admin = utilisateurRepository.findByEmail("womeloic@gmail.com").orElseThrow();
    }

    // ------------------------------------------------------------------ Adhesion

    @Test
    void leDirecteurEtLAdminVoientEtValidentLesDemandesDAdhesion() throws Exception {
        String etab = json(appeler(post("/api/etablissements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Clinique A\",\"ville\":\"Yaounde\"}"), directeur)).get("id").asText();
        String etabAdmin = json(appeler(post("/api/etablissements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Hopital central\"}"), admin)).get("id").asText();

        String d1 = json(appeler(post("/api/medecins/" + medecin.getId() + "/demander-integration/" + etab), medecin)
                .andExpect(status().isCreated())).get("id").asText();
        appeler(post("/api/medecins/" + autreMedecin.getId() + "/demander-integration/" + etabAdmin), autreMedecin)
                .andExpect(status().isCreated());

        JsonNode pourDirecteur = json(appeler(get("/api/demandes-integration/en-attente"), directeur).andExpect(status().isOk()));
        assertThat(pourDirecteur).hasSize(1);
        assertThat(pourDirecteur.get(0).get("medecinNumeroOrdre").asText()).isEqualTo("ONMC-123");
        assertThat(json(appeler(get("/api/demandes-integration/en-attente"), admin))).hasSize(2);
        appeler(get("/api/demandes-integration/en-attente"), medecin).andExpect(status().isForbidden());

        appeler(patch("/api/demandes-integration/" + d1 + "/repondre").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepter\":true}"), directeur).andExpect(status().isOk());
        assertThat(json(appeler(get("/api/demandes-integration/en-attente"), directeur))).isEmpty();
        assertThat(json(appeler(get("/api/medecins/" + medecin.getId()), patient).andExpect(status().isOk()))
                .get("etablissementId").asText()).isEqualTo(etab);
    }

    // ------------------------------------------------------------------ Disponibilites

    @Test
    void unRendezVousNePeutEtrePrisQueSurUnCreneauDuCalendrier() throws Exception {
        // Heures ouvrables par defaut : lundi-vendredi 08-12h / 14-17h, creneaux de 30 min
        JsonNode dispo = json(appeler(get("/api/disponibilites/medecins/" + medecin.getId()), patient).andExpect(status().isOk()));
        assertThat(dispo.get("parDefaut").asBoolean()).isTrue();
        JsonNode creneauxLundi = json(appeler(get("/api/disponibilites/medecins/" + medecin.getId() + "/creneaux")
                .param("du", lundi.toString()).param("au", lundi.toString()), patient));
        assertThat(creneauxLundi).hasSize(14);

        prendreRdv(lundi.atTime(12, 30)).andExpect(status().isBadRequest());
        prendreRdv(lundi.plusDays(5).atTime(9, 0)).andExpect(status().isBadRequest()); // samedi
        prendreRdv(lundi.atTime(9, 10)).andExpect(status().isBadRequest());
        prendreRdv(lundi.atTime(9, 0)).andExpect(status().isCreated());
        prendreRdv(lundi.atTime(9, 0)).andExpect(status().isBadRequest());

        JsonNode apres = json(appeler(get("/api/disponibilites/medecins/" + medecin.getId() + "/creneaux")
                .param("du", lundi.toString()).param("au", lundi.toString()), patient));
        assertThat(apres.get(2).get("debut").asText()).startsWith(lundi + "T09:00");
        assertThat(apres.get(2).get("libre").asBoolean()).isFalse();

        // Le medecin definit sa semaine : samedi matin uniquement, creneaux de 20 min
        String semaine = "{\"plages\":[{\"jourSemaine\":\"SATURDAY\",\"heureDebut\":\"09:00\",\"heureFin\":\"11:00\",\"dureeCreneauMinutes\":20}]}";
        appeler(put("/api/disponibilites/moi").contentType(MediaType.APPLICATION_JSON).content(semaine), patient)
                .andExpect(status().isForbidden());
        appeler(put("/api/disponibilites/moi").contentType(MediaType.APPLICATION_JSON).content(semaine), medecin)
                .andExpect(status().isOk());
        String chevauchement = "{\"plages\":[{\"jourSemaine\":\"MONDAY\",\"heureDebut\":\"09:00\",\"heureFin\":\"11:00\"},"
                + "{\"jourSemaine\":\"MONDAY\",\"heureDebut\":\"10:00\",\"heureFin\":\"12:00\"}]}";
        appeler(put("/api/disponibilites/moi").contentType(MediaType.APPLICATION_JSON).content(chevauchement), medecin)
                .andExpect(status().isBadRequest());

        LocalDate samedi = lundi.plusDays(5);
        prendreRdv(lundi.atTime(10, 0)).andExpect(status().isBadRequest());
        prendreRdv(samedi.atTime(9, 40)).andExpect(status().isCreated());

        // Absence : plus aucun creneau ce samedi
        appeler(post("/api/disponibilites/moi/absences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDebut\":\"" + samedi + "\",\"dateFin\":\"" + samedi + "\",\"motif\":\"Congres\"}"), medecin)
                .andExpect(status().isCreated());
        assertThat(json(appeler(get("/api/disponibilites/medecins/" + medecin.getId() + "/creneaux")
                .param("du", samedi.toString()).param("au", samedi.toString()), patient))).isEmpty();
        prendreRdv(samedi.atTime(10, 0)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ Documents medicaux

    @Test
    void lePatientScanneSesAntecedentsEtSeulsLesAyantsDroitLesLisent() throws Exception {
        MockMultipartFile donnees = new MockMultipartFile("donnees", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"type\":\"ANCIEN_CARNET\",\"titre\":\"Carnet 2019\",\"dateDocument\":\"2019-05-02\"}".getBytes());
        MockMultipartFile page1 = new MockMultipartFile("fichiers", "page1.png", "image/png", new byte[]{1, 2, 3});
        MockMultipartFile pdf = new MockMultipartFile("fichiers", "bilan.pdf", "application/pdf",
                "%PDF-1.4 contenu".getBytes(StandardCharsets.US_ASCII));
        MockMultipartFile fauxPdf = new MockMultipartFile("fichiers", "x.pdf", "application/pdf", "<html>".getBytes());

        String url = "/api/documents-medicaux/patients/" + patient.getId();
        appeler(multipart(url).file(donnees).file(fauxPdf), patient).andExpect(status().isBadRequest());
        appeler(multipart(url).file(donnees).file(page1), autrePatient).andExpect(status().isForbidden());
        // Un medecin sans autorisation d'ecriture ne peut pas ajouter de document
        appeler(multipart(url).file(donnees).file(page1), medecin).andExpect(status().isForbidden());

        JsonNode doc = json(appeler(multipart(url).file(donnees).file(page1).file(pdf), patient).andExpect(status().isCreated()));
        assertThat(doc.get("pages")).hasSize(2);
        String docId = doc.get("id").asText();

        appeler(get(url), autrePatient).andExpect(status().isForbidden());
        JsonNode vusParMedecin = json(appeler(get(url), medecin).andExpect(status().isOk()));
        assertThat(vusParMedecin).hasSize(1);
        assertThat(vusParMedecin.get(0).get("supprimable").asBoolean()).isFalse();

        byte[] contenu = appeler(get("/api/documents-medicaux/" + docId + "/pages/1"), medecin)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(contenu, StandardCharsets.US_ASCII)).startsWith("%PDF");
        appeler(get("/api/documents-medicaux/" + docId + "/pages/0"), autrePatient).andExpect(status().isForbidden());

        appeler(delete("/api/documents-medicaux/" + docId), medecin).andExpect(status().isForbidden());
        appeler(delete("/api/documents-medicaux/" + docId), patient).andExpect(status().isNoContent());
        assertThat(json(appeler(get(url), patient))).isEmpty();
    }

    // ------------------------------------------------------------------ Teleconsultation

    @Test
    void laSalleDeTeleconsultationRelaieLaSignalisationEntreLesDeuxParticipants() throws Exception {
        LocalDateTime maintenant = LocalDateTime.now(ZoneId.of("Africa/Douala")).truncatedTo(ChronoUnit.MINUTES);
        RendezVous enCours = rendezVousRepository.save(RendezVous.builder().patient(patient).medecin(medecin)
                .dateHeure(maintenant.plusMinutes(5)).type(TypeConsultation.TELECONSULTATION)
                .statut(StatutRendezVous.CONFIRME).build());
        RendezVous plusTard = rendezVousRepository.save(RendezVous.builder().patient(patient).medecin(medecin)
                .dateHeure(maintenant.plusDays(3)).type(TypeConsultation.TELECONSULTATION)
                .statut(StatutRendezVous.CONFIRME).build());

        JsonNode infos = json(appeler(get("/api/teleconsultations/" + enCours.getId()), patient).andExpect(status().isOk()));
        assertThat(infos.get("ouverte").asBoolean()).isTrue();
        assertThat(infos.get("monRole").asText()).isEqualTo("PATIENT");
        assertThat(json(appeler(get("/api/teleconsultations/" + enCours.getId()), medecin)).get("monRole").asText())
                .isEqualTo("MEDECIN");
        appeler(get("/api/teleconsultations/" + enCours.getId()), autreMedecin).andExpect(status().isForbidden());
        assertThat(json(appeler(get("/api/teleconsultations/" + plusTard.getId()), patient)).get("ouverte").asBoolean()).isFalse();

        SignalTeleconsultationRequest offre = SignalTeleconsultationRequest.builder()
                .type("offer").data(objectMapper.readTree("{\"sdp\":\"v=0\"}")).build();
        teleconsultationService.relayer(enCours.getId(), utilisateurRepository.findById(medecin.getId()).orElseThrow(), offre);
        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(eq("aline@test.cm"), eq(TeleconsultationService.QUEUE), message.capture());
        SignalTeleconsultationResponse recu = (SignalTeleconsultationResponse) message.getValue();
        assertThat(recu.getDeRole()).isEqualTo("MEDECIN");
        assertThat(recu.getData().get("sdp").asText()).isEqualTo("v=0");

        Utilisateur intrus = utilisateurRepository.findById(autrePatient.getId()).orElseThrow();
        assertThatThrownBy(() -> teleconsultationService.relayer(enCours.getId(), intrus, offre))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        Utilisateur lePatient = utilisateurRepository.findById(patient.getId()).orElseThrow();
        assertThatThrownBy(() -> teleconsultationService.relayer(plusTard.getId(), lePatient, offre))
                .isInstanceOf(com.medilinkpro.backend.exception.BadRequestException.class);
        verify(messagingTemplate, org.mockito.Mockito.times(1)).convertAndSendToUser(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void enFinDeTeleconsultationLeMedecinPrescritUneOrdonnance() throws Exception {
        RendezVous rdv = rendezVousRepository.save(RendezVous.builder().patient(patient).medecin(medecin)
                .dateHeure(LocalDateTime.now().minusMinutes(20)).type(TypeConsultation.TELECONSULTATION)
                .statut(StatutRendezVous.CONFIRME).build());
        String cloture = objectMapper.writeValueAsString(Map.of("motif", "Fievre", "diagnostic", "Paludisme simple",
                "compteRendu", "Patient febrile depuis 2 jours", "medicaments", "Artemether-Lumefantrine", "posologie", "2 cp matin et soir, 3 jours"));

        appeler(post("/api/teleconsultations/" + rdv.getId() + "/cloture").contentType(MediaType.APPLICATION_JSON).content(cloture), patient)
                .andExpect(status().isForbidden());
        JsonNode r = json(appeler(post("/api/teleconsultations/" + rdv.getId() + "/cloture")
                .contentType(MediaType.APPLICATION_JSON).content(cloture), medecin).andExpect(status().isCreated()));
        assertThat(r.get("ordonnance").get("medicaments").asText()).isEqualTo("Artemether-Lumefantrine");
        appeler(post("/api/teleconsultations/" + rdv.getId() + "/cloture").contentType(MediaType.APPLICATION_JSON).content(cloture), medecin)
                .andExpect(status().isConflict());

        assertThat(rendezVousRepository.findById(rdv.getId()).orElseThrow().getStatut()).isEqualTo(StatutRendezVous.TERMINE);
        assertThat(json(appeler(get("/api/ordonnances/patient/" + patient.getId()), patient))).hasSize(1);
        assertThat(json(appeler(get("/api/notifications"), patient)).get(0).get("titre").asText()).isEqualTo("Votre ordonnance est disponible");
    }

    // ------------------------------------------------------------------ Outils

    private ResultActions prendreRdv(LocalDateTime dateHeure) throws Exception {
        return appeler(post("/api/rendez-vous").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                "patientId", patient.getId(), "medecinId", medecin.getId(), "dateHeure", dateHeure.toString()))), patient);
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, Utilisateur u) throws Exception {
        return mockMvc.perform(requete.header("Authorization",
                "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow())));
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }
}

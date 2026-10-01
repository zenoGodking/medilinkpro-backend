package com.medilinkpro.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.Directeur;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeConsultation;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class StatistiquesTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired EtablissementRepository etablissementRepository;
    @Autowired RendezVousRepository rendezVousRepository;
    @Autowired JwtService jwtService;

    @Test
    void leDirecteurNeVoitQueLActiviteDeSesEtablissements() throws Exception {
        Directeur dir = utilisateurRepository.save(Directeur.builder().nom("Dir").prenom("A").email("dir@test.cm").motDePasse("x")
                .role(Role.DIRECTEUR).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        EtablissementSante clinique = etablissementRepository.save(EtablissementSante.builder().nom("Clinique A").directeur(dir).build());
        Medecin sien = utilisateurRepository.save(Medecin.builder().nom("Kamga").prenom("Paul").email("kamga@test.cm").motDePasse("x")
                .etablissement(clinique).role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        Medecin autre = utilisateurRepository.save(Medecin.builder().nom("Etoa").prenom("Jean").email("etoa@test.cm").motDePasse("x")
                .role(Role.MEDECIN).statutCompte(StatutCompte.APPROUVE).actif(true).build());
        Patient patient = utilisateurRepository.save(Patient.builder().nom("Mballa").prenom("Aline").email("aline@test.cm").motDePasse("x")
                .role(Role.PATIENT).statutCompte(StatutCompte.APPROUVE).actif(true).build());

        LocalDateTime hier = LocalDateTime.now().minusDays(1);
        rdv(patient, sien, hier, StatutRendezVous.TERMINE, TypeConsultation.PHYSIQUE);
        rdv(patient, sien, hier.minusDays(2), StatutRendezVous.TERMINE, TypeConsultation.TELECONSULTATION);
        rdv(patient, sien, hier.minusDays(3), StatutRendezVous.NO_SHOW, TypeConsultation.PHYSIQUE);
        rdv(patient, sien, hier.minusDays(4), StatutRendezVous.TERMINE, TypeConsultation.PHYSIQUE);
        rdv(patient, sien, LocalDateTime.now().plusDays(2), StatutRendezVous.EN_ATTENTE, TypeConsultation.PHYSIQUE);
        rdv(patient, autre, hier, StatutRendezVous.TERMINE, TypeConsultation.PHYSIQUE);

        JsonNode s = json(dir);
        assertThat(s.get("medecins").asLong()).isEqualTo(1);
        assertThat(s.get("rendezVous").asLong()).isEqualTo(4);
        assertThat(s.get("tauxAbsence").asDouble()).isEqualTo(25.0);
        assertThat(s.get("teleconsultations").asLong()).isEqualTo(1);
        assertThat(s.get("demandesEnAttente").asLong()).isEqualTo(1);
        assertThat(s.get("rendezVousParJour")).hasSize(30);
        assertThat(s.get("medecinsLesPlusSollicites").get(0).get("nomComplet").asText()).isEqualTo("Dr Paul Kamga");

        Utilisateur admin = utilisateurRepository.findByEmail("womeloic@gmail.com").orElseThrow();
        assertThat(json(admin).get("rendezVous").asLong()).isEqualTo(5);
        mockMvc.perform(get("/api/dashboard/statistiques").header("Authorization", "Bearer " + jwtService.generateToken(sien)))
                .andExpect(status().isForbidden());
    }

    private void rdv(Patient p, Medecin m, LocalDateTime quand, StatutRendezVous statut, TypeConsultation type) {
        rendezVousRepository.save(RendezVous.builder().patient(p).medecin(m).dateHeure(quand).statut(statut).type(type).build());
    }

    private JsonNode json(Utilisateur u) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/dashboard/statistiques")
                        .header("Authorization", "Bearer " + jwtService.generateToken(utilisateurRepository.findById(u.getId()).orElseThrow())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}

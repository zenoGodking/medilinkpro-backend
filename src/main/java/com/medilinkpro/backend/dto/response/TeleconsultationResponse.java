package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.StatutRendezVous;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeleconsultationResponse {

    private UUID rendezVousId;
    private LocalDateTime dateHeure;
    private StatutRendezVous statut;

    private UUID medecinId;
    private String medecinNomComplet;
    private String specialiteMedecin;
    private UUID patientId;
    private String patientNomComplet;

    /** MEDECIN ou PATIENT : role de l'utilisateur connecte dans cette teleconsultation. */
    private String monRole;

    private LocalDateTime ouvertureA;
    private LocalDateTime fermetureA;
    /** true si la salle est ouverte maintenant (rendez-vous confirme et dans la fenetre horaire). */
    private boolean ouverte;
    /** Raison pour laquelle la salle est fermee, le cas echeant. */
    private String raison;
}

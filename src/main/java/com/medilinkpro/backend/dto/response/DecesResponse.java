package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.entity.NotificationSms;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecesResponse {

    private PatientResponse patient;
    /** Numero du proche a informer ; null si le patient n'en avait pas renseigne. */
    private String procheTelephone;
    /** Resultat de la notification du proche ; null si aucun numero. */
    private NotificationSms.Statut statutNotification;
}

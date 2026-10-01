package com.medilinkpro.backend.service.sms;

import com.medilinkpro.backend.entity.NotificationSms;
import com.medilinkpro.backend.repository.NotificationSmsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Envoie un SMS via le FournisseurSms configure et en garde une trace en base.
 * Tant qu'aucun fournisseur n'est branche, le message est seulement journalise et
 * enregistre avec le statut NON_ENVOYE_AUCUN_FOURNISSEUR : rien ne part reellement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSmsService {

    /**
     * Ramene le texte a l'alphabet SMS standard (GSM 03.38) : un seul caractere hors de cet alphabet
     * (ex: "e" circonflexe) fait passer le SMS en Unicode, limite a 70 caracteres au lieu de 160, donc
     * facture double. On garde les accents presents dans l'alphabet (e aigu, e grave, a grave...).
     */
    static String versAlphabetSms(String texte) {
        if (texte == null) {
            return null;
        }
        StringBuilder sortie = new StringBuilder(texte.length());
        for (char c : texte.toCharArray()) {
            if ("éèùìòàÉÇÄÖÑÜäöñü".indexOf(c) >= 0 || c < 128) {
                sortie.append(c);
                continue;
            }
            switch (c) {
                case 'ê', 'ë' -> sortie.append('e');
                case 'â' -> sortie.append('a');
                case 'î', 'ï' -> sortie.append('i');
                case 'ô' -> sortie.append('o');
                case 'û' -> sortie.append('u');
                case 'ç' -> sortie.append('c');
                case 'È', 'Ê' -> sortie.append('E');
                case 'À', 'Â' -> sortie.append('A');
                case 'Ô' -> sortie.append('O');
                case 'œ' -> sortie.append("oe");
                case '«', '»' -> sortie.append('"');
                case '’' -> sortie.append('\'');
                default -> {
                    String decompose = java.text.Normalizer.normalize(String.valueOf(c), java.text.Normalizer.Form.NFD)
                            .replaceAll("\\p{M}", "");
                    sortie.append(decompose.chars().allMatch(x -> x < 128) ? decompose : "?");
                }
            }
        }
        return sortie.toString();
    }

    private final NotificationSmsRepository notificationSmsRepository;
    private final ObjectProvider<FournisseurSms> fournisseurSms;

    public NotificationSms envoyer(String numero, String message, String contexte, UUID patientId) {
        return envoyer(numero, message, contexte, patientId, message);
    }

    /**
     * @param messageArchive version du message conservee en base et dans les journaux : permet de ne
     *                       jamais y laisser un secret (ex: code de reinitialisation masque).
     */
    public NotificationSms envoyer(String numero, String message, String contexte, UUID patientId, String messageArchive) {
        NotificationSms.Statut statut;
        FournisseurSms fournisseur = fournisseurSms.getIfAvailable();
        if (fournisseur == null) {
            log.warn("SMS NON ENVOYÉ (aucun fournisseur configuré) à {} : {}", numero, messageArchive);
            statut = NotificationSms.Statut.NON_ENVOYE_AUCUN_FOURNISSEUR;
        } else {
            try {
                fournisseur.envoyer(numero, versAlphabetSms(message));
                statut = NotificationSms.Statut.ENVOYE;
            } catch (RuntimeException e) {
                log.error("Échec d'envoi du SMS à {}", numero, e);
                statut = NotificationSms.Statut.ECHEC;
            }
        }
        return notificationSmsRepository.save(NotificationSms.builder()
                .destinataire(numero)
                .message(messageArchive)
                .contexte(contexte)
                .patientId(patientId)
                .statut(statut)
                .build());
    }
}

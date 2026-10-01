package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.DemandeIntegrationRequest;
import com.medilinkpro.backend.dto.request.ReponseDemandeIntegrationRequest;
import com.medilinkpro.backend.dto.response.DemandeIntegrationResponse;
import com.medilinkpro.backend.entity.DemandeIntegration;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.InitiateurDemande;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutDemandeIntegration;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ConflictException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.DemandeIntegrationRepository;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.InfirmierRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Demandes d'integration d'un medecin dans un etablissement (F-Integration) :
 * - Un Directeur/Admin invite un medecin -> le medecin doit valider ou refuser.
 * - Un medecin demande a rejoindre un etablissement -> le Directeur/Admin doit valider ou refuser.
 * Une fois acceptee, le medecin est rattache a l'etablissement.
 */
@Service
@RequiredArgsConstructor
public class DemandeIntegrationService {

    private final DemandeIntegrationRepository demandeRepository;
    private final MedecinRepository medecinRepository;
    private final EtablissementRepository etablissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final InfirmierRepository infirmierRepository;

    /** L'etablissement (Directeur/Admin) invite un medecin a le rejoindre. */
    @Transactional
    public DemandeIntegrationResponse inviter(UUID etablissementId, UUID medecinId, DemandeIntegrationRequest request) {
        return creerDemande(medecinId, etablissementId, InitiateurDemande.ETABLISSEMENT, request.getMessage());
    }

    /** Le medecin demande a rejoindre un etablissement. */
    @Transactional
    public DemandeIntegrationResponse demander(UUID medecinId, UUID etablissementId, DemandeIntegrationRequest request) {
        return creerDemande(medecinId, etablissementId, InitiateurDemande.MEDECIN, request.getMessage());
    }

    /** Une infirmiere demande a rejoindre un etablissement : le directeur (ou l'admin) valide. */
    @Transactional
    public DemandeIntegrationResponse demanderInfirmier(UUID infirmierId, UUID etablissementId, DemandeIntegrationRequest request) {
        Infirmier infirmier = infirmierRepository.findById(infirmierId)
                .orElseThrow(() -> new ResourceNotFoundException("Infirmier non trouvé avec l'id : " + infirmierId));
        EtablissementSante etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement non trouvé avec l'id : " + etablissementId));

        if (infirmier.getEtablissement() != null && infirmier.getEtablissement().getId().equals(etablissementId)) {
            throw new BadRequestException("Vous faites déjà partie de cet établissement");
        }
        if (demandeRepository.existsByInfirmierIdAndEtablissementIdAndStatut(
                infirmierId, etablissementId, StatutDemandeIntegration.EN_ATTENTE)) {
            throw new ConflictException("Une demande est déjà en attente pour cet établissement");
        }

        return toResponse(demandeRepository.save(DemandeIntegration.builder()
                .infirmier(infirmier)
                .etablissement(etablissement)
                .initiateur(InitiateurDemande.INFIRMIER)
                .statut(StatutDemandeIntegration.EN_ATTENTE)
                .message(request.getMessage())
                .build()));
    }

    @Transactional(readOnly = true)
    public List<DemandeIntegrationResponse> listerParInfirmier(UUID infirmierId) {
        return demandeRepository.findByInfirmierIdOrderByDateCreationDesc(infirmierId)
                .stream().map(this::toResponse).toList();
    }

    private DemandeIntegrationResponse creerDemande(UUID medecinId, UUID etablissementId,
                                                      InitiateurDemande initiateur, String message) {
        Medecin medecin = medecinRepository.findById(medecinId)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin non trouvé avec l'id : " + medecinId));
        EtablissementSante etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement non trouvé avec l'id : " + etablissementId));

        if (medecin.getEtablissement() != null && medecin.getEtablissement().getId().equals(etablissementId)) {
            throw new BadRequestException("Ce médecin fait déjà partie de cet établissement");
        }
        boolean dejaEnAttente = demandeRepository.existsByMedecinIdAndEtablissementIdAndStatut(
                medecinId, etablissementId, StatutDemandeIntegration.EN_ATTENTE);
        if (dejaEnAttente) {
            throw new ConflictException("Une demande est déjà en attente entre ce médecin et cet établissement");
        }

        DemandeIntegration demande = DemandeIntegration.builder()
                .medecin(medecin)
                .etablissement(etablissement)
                .initiateur(initiateur)
                .statut(StatutDemandeIntegration.EN_ATTENTE)
                .message(message)
                .build();

        return toResponse(demandeRepository.save(demande));
    }

    /**
     * Repond a une demande. Si l'etablissement etait a l'origine, seul le medecin concerne
     * peut repondre. Si le medecin etait a l'origine, seul le directeur de l'etablissement ou un Admin peut repondre.
     */
    @Transactional
    public DemandeIntegrationResponse repondre(UUID demandeId, UUID actorId, ReponseDemandeIntegrationRequest request) {
        DemandeIntegration demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

        if (demande.getStatut() != StatutDemandeIntegration.EN_ATTENTE) {
            throw new BadRequestException("Cette demande a déjà été traitée");
        }

        Utilisateur acteur = utilisateurRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        boolean autorise = switch (demande.getInitiateur()) {
            case ETABLISSEMENT -> demande.getMedecin().getId().equals(actorId) || acteur.getRole() == Role.ADMIN;
            // Seul le directeur de CET etablissement (ou l'admin) accepte un professionnel qui demande a le rejoindre.
            case MEDECIN, INFIRMIER -> acteur.getRole() == Role.ADMIN
                    || (acteur.getRole() == Role.DIRECTEUR
                        && demande.getEtablissement().getDirecteur() != null
                        && demande.getEtablissement().getDirecteur().getId().equals(actorId));
        };
        if (!autorise) {
            throw new org.springframework.security.access.AccessDeniedException("Vous n'êtes pas autorisé à répondre à cette demande");
        }

        if (Boolean.TRUE.equals(request.getAccepter())) {
            demande.setStatut(StatutDemandeIntegration.ACCEPTEE);
            if (demande.getInfirmier() != null) {
                Infirmier infirmier = demande.getInfirmier();
                infirmier.setEtablissement(demande.getEtablissement());
                infirmierRepository.save(infirmier);
            } else {
                Medecin medecin = demande.getMedecin();
                medecin.setEtablissement(demande.getEtablissement());
                medecinRepository.save(medecin);
            }
        } else {
            demande.setStatut(StatutDemandeIntegration.REFUSEE);
        }
        demande.setMessageReponse(request.getMessageReponse());
        demande.setDateReponse(LocalDateTime.now());

        return toResponse(demandeRepository.save(demande));
    }

    @Transactional(readOnly = true)
    public List<DemandeIntegrationResponse> listerParMedecin(UUID medecinId) {
        return demandeRepository.findByMedecinIdOrderByDateCreationDesc(medecinId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeIntegrationResponse> listerParEtablissement(UUID etablissementId) {
        return demandeRepository.findByEtablissementIdOrderByDateCreationDesc(etablissementId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Demandes en attente a traiter : toutes pour l'admin, celles de ses etablissements pour un directeur.
     * Les demandes initiees par un medecin sont celles que le directeur/admin doit valider.
     */
    @Transactional(readOnly = true)
    public List<DemandeIntegrationResponse> listerEnAttente(Utilisateur u) {
        List<DemandeIntegration> demandes = switch (u.getRole()) {
            case ADMIN -> demandeRepository.findByStatutOrderByDateCreationDesc(StatutDemandeIntegration.EN_ATTENTE);
            case DIRECTEUR -> demandeRepository.findByEtablissement_Directeur_IdAndStatutOrderByDateCreationDesc(
                    u.getId(), StatutDemandeIntegration.EN_ATTENTE);
            default -> throw new org.springframework.security.access.AccessDeniedException("Réservé au directeur et à l'administrateur");
        };
        return demandes.stream().map(this::toResponse).toList();
    }

    private DemandeIntegrationResponse toResponse(DemandeIntegration d) {
        DemandeIntegrationResponse.DemandeIntegrationResponseBuilder builder = DemandeIntegrationResponse.builder()
                .id(d.getId())
                .etablissementId(d.getEtablissement().getId())
                .etablissementNom(d.getEtablissement().getNom())
                .initiateur(d.getInitiateur())
                .statut(d.getStatut())
                .message(d.getMessage())
                .messageReponse(d.getMessageReponse())
                .dateCreation(d.getDateCreation())
                .dateReponse(d.getDateReponse());

        Medecin m = d.getMedecin();
        if (m != null) {
            builder.typeProfessionnel("MEDECIN")
                    .professionnelId(m.getId()).professionnelNom(m.getNom()).professionnelPrenom(m.getPrenom())
                    .professionnelEmail(m.getEmail()).professionnelTelephone(m.getTelephone())
                    .medecinId(m.getId())
                    .medecinNom(m.getNom())
                    .medecinPrenom(m.getPrenom())
                    .medecinSpecialite(m.getSpecialite())
                    .medecinNumeroOrdre(m.getNumeroOrdre())
                    .medecinEmail(m.getEmail())
                    .medecinTelephone(m.getTelephone());
        }
        Infirmier i = d.getInfirmier();
        if (i != null) {
            builder.typeProfessionnel("INFIRMIER")
                    .professionnelId(i.getId()).professionnelNom(i.getNom()).professionnelPrenom(i.getPrenom())
                    .professionnelEmail(i.getEmail()).professionnelTelephone(i.getTelephone())
                    .infirmierId(i.getId())
                    .infirmierPhotoDisponible(i.getPhotoProfilChemin() != null);
        }
        return builder.build();
    }
}

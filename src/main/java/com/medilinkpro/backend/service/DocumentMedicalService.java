package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.DocumentMedicalRequest;
import com.medilinkpro.backend.dto.response.DocumentMedicalResponse;
import com.medilinkpro.backend.entity.DocumentMedical;
import com.medilinkpro.backend.entity.PageDocument;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.DocumentMedicalRepository;
import com.medilinkpro.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Documents medicaux importes dans le carnet (antecedents, anciens carnets scannes...).
 * - Ajout : le patient pour lui-meme, ou un medecin ayant le droit d'ecrire dans son carnet ;
 * - Lecture : memes regles que le carnet (CarnetAccesService.verifierLecture) ;
 * - Suppression : le patient, ou l'auteur du document.
 */
@Service
@RequiredArgsConstructor
public class DocumentMedicalService {

    public static final int PAGES_MAX = 20;
    private static final String DOSSIER = "documents-medicaux";

    private final DocumentMedicalRepository documentRepository;
    private final PatientRepository patientRepository;
    private final CarnetAccesService carnetAccesService;
    private final FileStorageService fileStorageService;

    public record Fichier(byte[] contenu, String typeMime, String nom) {}

    @Transactional
    public DocumentMedicalResponse ajouter(UUID patientId, DocumentMedicalRequest request,
                                           List<MultipartFile> fichiers, Utilisateur auteur) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouvé avec l'id : " + patientId));
        boolean estLePatient = auteur.getRole() == Role.PATIENT && auteur.getId().equals(patientId);
        if (!estLePatient) {
            carnetAccesService.verifierEcriture(auteur, patientId);
        } else if (patient.isDecede()) {
            throw new BadRequestException("Ce carnet est clos");
        }
        if (fichiers == null || fichiers.isEmpty()) {
            throw new BadRequestException("Ajoutez au moins un fichier (photo ou PDF)");
        }
        if (fichiers.size() > PAGES_MAX) {
            throw new BadRequestException("Un document contient au plus " + PAGES_MAX + " fichiers");
        }

        List<PageDocument> pages = new ArrayList<>();
        try {
            for (MultipartFile f : fichiers) {
                pages.add(PageDocument.builder()
                        .chemin(fileStorageService.storePrivateDocument(f, DOSSIER + "/" + patientId))
                        .nomOriginal(f.getOriginalFilename())
                        .typeMime(f.getContentType())
                        .taille(f.getSize())
                        .build());
            }
        } catch (RuntimeException e) {
            pages.forEach(p -> fileStorageService.deletePrivate(p.getChemin()));
            throw e;
        }
        // Si la transaction echoue apres l'ecriture des fichiers, on ne laisse pas d'orphelins sur disque.
        supprimerFichiersSiRollback(pages);

        DocumentMedical document = documentRepository.save(DocumentMedical.builder()
                .patient(patient)
                .type(request.getType())
                .titre(request.getTitre().trim())
                .description(request.getDescription())
                .dateDocument(request.getDateDocument())
                .pages(pages)
                .ajoutePar(auteur)
                .build());
        return toResponse(document, auteur);
    }

    @Transactional(readOnly = true)
    public List<DocumentMedicalResponse> lister(UUID patientId, Utilisateur lecteur) {
        carnetAccesService.verifierLecture(lecteur, patientId);
        return documentRepository.findByPatientIdOrderByDateAjoutDesc(patientId).stream()
                .map(d -> toResponse(d, lecteur)).toList();
    }

    @Transactional(readOnly = true)
    public Fichier lireFichier(UUID documentId, int index, Utilisateur lecteur) {
        DocumentMedical document = getOrThrow(documentId);
        carnetAccesService.verifierLecture(lecteur, document.getPatient().getId());
        if (index < 0 || index >= document.getPages().size()) {
            throw new ResourceNotFoundException("Page introuvable");
        }
        PageDocument page = document.getPages().get(index);
        return new Fichier(fileStorageService.readPrivate(page.getChemin()), page.getTypeMime(), page.getNomOriginal());
    }

    @Transactional
    public void supprimer(UUID documentId, Utilisateur utilisateur) {
        DocumentMedical document = getOrThrow(documentId);
        if (!peutSupprimer(document, utilisateur)) {
            throw new AccessDeniedException("Seul le patient ou l'auteur du document peut le supprimer");
        }
        List<String> chemins = document.getPages().stream().map(PageDocument::getChemin).toList();
        documentRepository.delete(document);
        apresCommit(() -> chemins.forEach(fileStorageService::deletePrivate));
    }

    private static boolean peutSupprimer(DocumentMedical d, Utilisateur u) {
        return u.getRole() == Role.ADMIN
                || u.getId().equals(d.getPatient().getId())
                || (d.getAjoutePar() != null && u.getId().equals(d.getAjoutePar().getId()));
    }

    private DocumentMedical getOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));
    }

    private void supprimerFichiersSiRollback(List<PageDocument> pages) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    pages.forEach(p -> fileStorageService.deletePrivate(p.getChemin()));
                }
            }
        });
    }

    private static void apresCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private DocumentMedicalResponse toResponse(DocumentMedical d, Utilisateur lecteur) {
        Utilisateur auteur = d.getAjoutePar();
        List<PageDocument> pages = d.getPages();
        return DocumentMedicalResponse.builder()
                .id(d.getId())
                .patientId(d.getPatient().getId())
                .type(d.getType())
                .titre(d.getTitre())
                .description(d.getDescription())
                .dateDocument(d.getDateDocument())
                .dateAjout(d.getDateAjout())
                .ajouteParId(auteur != null ? auteur.getId() : null)
                .ajouteParNom(auteur == null ? null
                        : (auteur.getRole() == Role.MEDECIN ? "Dr " : "") + auteur.getPrenom() + " " + auteur.getNom())
                .supprimable(peutSupprimer(d, lecteur))
                .pages(IntStream.range(0, pages.size()).mapToObj(i -> DocumentMedicalResponse.Page.builder()
                        .index(i)
                        .nomOriginal(pages.get(i).getNomOriginal())
                        .typeMime(pages.get(i).getTypeMime())
                        .taille(pages.get(i).getTaille())
                        .build()).toList())
                .build();
    }
}

package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.LoginRequest;
import com.medilinkpro.backend.dto.request.RegisterRequest;
import com.medilinkpro.backend.dto.response.AuthResponse;
import com.medilinkpro.backend.entity.*;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.CompteNonValideException;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service d'authentification : inscription multi-role et connexion (login) avec emission d'un JWT.
 * Le type concret d'Utilisateur cree depend du role transmis dans la requete d'inscription.
 * Pour un Patient, un DossierMedical (DME) vide est automatiquement cree (F01),
 * conformement a l'association "1 Patient -> 1 DossierMedical" du diagramme de classes.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final DossierMedicalRepository dossierMedicalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ReconnaissanceFacialeService reconnaissanceFacialeService;
    private final FileStorageService fileStorageService;
    private final com.medilinkpro.backend.securite.LimiteurTentatives limiteur;

    private static final java.time.Duration FENETRE_CONNEXION = java.time.Duration.ofMinutes(15);

    /**
     * @param photo       photo du visage, obligatoire pour un patient (reconnaissance faciale en urgence)
     *                    et pour une infirmiere (photo de profil presentee aux patients)
     * @param descripteur empreinte faciale calculee par le navigateur a partir de cette photo
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, MultipartFile photo, List<Double> descripteur) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = buildUtilisateur(request);
        if (utilisateur instanceof Patient patient) {
            if (photo == null || descripteur == null) {
                throw new BadRequestException("Une photo du visage est obligatoire pour l'inscription d'un patient");
            }
            reconnaissanceFacialeService.enroler(patient, photo, descripteur);
        }
        if (utilisateur instanceof Infirmier infirmier) {
            // Le patient doit pouvoir reconnaitre l'infirmiere qui vient chez lui.
            if (photo == null || photo.isEmpty()) {
                throw new BadRequestException("Une photo de profil est obligatoire pour l'inscription d'une infirmière");
            }
            infirmier.setPhotoProfilChemin(fileStorageService.storePrivateImage(photo, InfirmierService.DOSSIER_PHOTOS));
        }
        Utilisateur saved = utilisateurRepository.save(utilisateur);

        if (saved instanceof Patient patient) {
            DossierMedical dossier = DossierMedical.builder()
                    .patient(patient)
                    .chiffrementActif(true)
                    .build();
            dossierMedicalRepository.save(dossier);
        }

        // Medecin, Infirmier et Directeur doivent etre valides par un Admin avant de pouvoir
        // se connecter : on ne genere pas de token, on renvoie un message d'attente a la place.
        if (saved.getStatutCompte() == StatutCompte.EN_ATTENTE) {
            return AuthResponse.builder()
                    .userId(saved.getId())
                    .email(saved.getEmail())
                    .nom(saved.getNom())
                    .prenom(saved.getPrenom())
                    .role(saved.getRole())
                    .message("Votre demande d'inscription a bien été reçue. Un administrateur doit valider votre compte avant votre première connexion.")
                    .build();
        }

        String token = jwtService.generateToken(saved);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(saved.getId())
                .email(saved.getEmail())
                .nom(saved.getNom())
                .prenom(saved.getPrenom())
                .role(saved.getRole())
                .build();
    }

    /**
     * Connexion. Apres 5 echecs en 15 minutes sur un meme email (ou 20 depuis une meme adresse IP),
     * les tentatives sont refusees jusqu'a la fin de la fenetre (protection contre la force brute).
     */
    public AuthResponse login(LoginRequest request, String ip) {
        String cleEmail = "login:" + ReinitialisationMotDePasseService.normaliser(request.getEmail());
        String cleIp = "login-ip:" + ip;
        String message = "Trop de tentatives de connexion. Réessayez dans 15 minutes ou réinitialisez votre mot de passe.";
        limiteur.verifier(cleEmail, 5, FENETRE_CONNEXION, message);
        limiteur.verifier(cleIp, 20, FENETRE_CONNEXION, message);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
            );
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            limiteur.enregistrer(cleEmail, FENETRE_CONNEXION);
            limiteur.enregistrer(cleIp, FENETRE_CONNEXION);
            throw e;
        }
        limiteur.reinitialiser(cleEmail);

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email ou mot de passe incorrect"));

        if (utilisateur.getStatutCompte() == StatutCompte.EN_ATTENTE) {
            throw new CompteNonValideException("Votre compte est en attente de validation par un administrateur.");
        }
        if (utilisateur.getStatutCompte() == StatutCompte.REJETE) {
            throw new CompteNonValideException("Votre demande d'inscription a été refusée. Contactez un administrateur pour plus d'informations.");
        }

        String token = jwtService.generateToken(utilisateur);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(utilisateur.getId())
                .email(utilisateur.getEmail())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole())
                .build();
    }

    private Utilisateur buildUtilisateur(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getMotDePasse());
        Role role = request.getRole();
        // Patient et Admin sont operationnels immediatement ; les profils professionnels
        // (Medecin, Infirmier, Directeur) doivent d'abord etre valides par un Admin.
        StatutCompte statutCompte = (role == Role.PATIENT || role == Role.ADMIN)
                ? StatutCompte.APPROUVE
                : StatutCompte.EN_ATTENTE;

        return switch (role) {
            case PATIENT -> Patient.builder()
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .motDePasse(encodedPassword)
                    .telephone(request.getTelephone())
                    .actif(true)
                    .role(Role.PATIENT)
                    .statutCompte(statutCompte)
                    .dateNaissance(request.getDateNaissance())
                    .groupeSanguin(request.getGroupeSanguin())
                    .allergies(request.getAllergies())
                    .antecedents(request.getAntecedents())
                    .conditionsUrgence(request.getConditionsUrgence())
                    .numSecuriteSociale(request.getNumSecuriteSociale())
                    .contactUrgenceNom(request.getContactUrgenceNom())
                    .contactUrgenceTelephone(request.getContactUrgenceTelephone())
                    .build();

            case MEDECIN -> Medecin.builder()
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .motDePasse(encodedPassword)
                    .telephone(request.getTelephone())
                    .actif(true)
                    .role(Role.MEDECIN)
                    .statutCompte(statutCompte)
                    .specialite(request.getSpecialite())
                    .numeroOrdre(request.getNumeroOrdre())
                    .ville(request.getVille())
                    .quartier(request.getQuartier())
                    .tarif(request.getTarif())
                    .verifie(false)
                    .build();

            case ADMIN -> Admin.builder()
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .motDePasse(encodedPassword)
                    .telephone(request.getTelephone())
                    .actif(true)
                    .role(Role.ADMIN)
                    .statutCompte(statutCompte)
                    .build();

            case DIRECTEUR -> Directeur.builder()
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .motDePasse(encodedPassword)
                    .telephone(request.getTelephone())
                    .actif(true)
                    .role(Role.DIRECTEUR)
                    .statutCompte(statutCompte)
                    .build();


            case PHARMACIEN -> Pharmacien.builder()
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .motDePasse(encodedPassword)
                    .telephone(request.getTelephone())
                    .actif(true)
                    .role(Role.PHARMACIEN)
                    .statutCompte(statutCompte)
                    .nomPharmacie(request.getNomPharmacie())
                    .numeroAgrement(request.getNumeroAgrement())
                    .ville(request.getVille())
                    .build();

            case INFIRMIER -> Infirmier.builder()
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .motDePasse(encodedPassword)
                    .telephone(request.getTelephone())
                    .actif(true)
                    .role(Role.INFIRMIER)
                    .statutCompte(statutCompte)
                    .build();
        };
    }
}

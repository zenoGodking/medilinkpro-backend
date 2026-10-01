package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.LoginRequest;
import com.medilinkpro.backend.dto.request.RegisterRequest;
import com.medilinkpro.backend.dto.response.AuthResponse;
import com.medilinkpro.backend.service.AuthService;
import com.medilinkpro.backend.util.DescripteursJson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.medilinkpro.backend.dto.request.MotDePasseOublieRequest;
import com.medilinkpro.backend.dto.request.ReinitialisationMotDePasseRequest;
import com.medilinkpro.backend.service.ReinitialisationMotDePasseService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion multi-role (Patient, Medecin, Infirmier, Admin, Directeur)")
public class AuthController {

    private final AuthService authService;
    private final DescripteursJson descripteursJson;
    private final ReinitialisationMotDePasseService reinitialisationService;

    @PostMapping(value = "/register", consumes = "application/json")
    @SecurityRequirements
    @Operation(summary = "Inscription d'un professionnel", description = "Cree un compte selon le role indique. "
            + "Un PATIENT ou une INFIRMIERE doit s'inscrire via la variante multipart (photo obligatoire).")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, null, null));
    }

    @PostMapping(value = "/register", consumes = "multipart/form-data")
    @SecurityRequirements
    @Operation(summary = "Inscription avec photo faciale", description = "Parties : donnees (JSON RegisterRequest), "
            + "photo (image du visage), descripteur (JSON, 128 réels calculés par face-api, obligatoire pour un PATIENT). "
            + "Obligatoire pour un PATIENT et une INFIRMIÈRE (photo de profil).")
    public ResponseEntity<AuthResponse> registerAvecPhoto(
            @Valid @RequestPart("donnees") RegisterRequest request,
            @RequestPart("photo") MultipartFile photo,
            @RequestPart(value = "descripteur", required = false) String descripteur) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request, photo, descripteur == null ? null : descripteursJson.lire(descripteur)));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Connexion", description = "Authentifie un utilisateur et retourne un token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, http.getRemoteAddr()));
    }

    @PostMapping("/mot-de-passe-oublie")
    @SecurityRequirements
    @Operation(summary = "Mot de passe oublie : envoie un code a 6 chiffres par SMS au numero du compte",
            description = "Reponse identique que le compte existe ou non. Limite a 3 demandes par heure et par email.")
    public ResponseEntity<Map<String, String>> motDePasseOublie(@Valid @RequestBody MotDePasseOublieRequest request,
                                                                HttpServletRequest http) {
        reinitialisationService.demander(request.getEmail(), http.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", ReinitialisationMotDePasseService.MESSAGE_ENVOI));
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    @SecurityRequirements
    @Operation(summary = "Definir un nouveau mot de passe avec le code recu par SMS (5 essais par code)")
    public ResponseEntity<Map<String, String>> reinitialiser(@Valid @RequestBody ReinitialisationMotDePasseRequest request) {
        reinitialisationService.reinitialiser(request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié. Vous pouvez vous connecter."));
    }
}

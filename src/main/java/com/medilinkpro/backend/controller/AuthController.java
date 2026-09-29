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

    @PostMapping(value = "/register", consumes = "application/json")
    @SecurityRequirements
    @Operation(summary = "Inscription d'un professionnel", description = "Cree un compte selon le role indique. "
            + "Un PATIENT doit s'inscrire via la variante multipart (photo faciale obligatoire).")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, null, null));
    }

    @PostMapping(value = "/register", consumes = "multipart/form-data")
    @SecurityRequirements
    @Operation(summary = "Inscription avec photo faciale", description = "Parties : donnees (JSON RegisterRequest), "
            + "photo (image du visage), descripteur (JSON, 128 reels calcules par face-api). Obligatoire pour un PATIENT.")
    public ResponseEntity<AuthResponse> registerAvecPhoto(
            @Valid @RequestPart("donnees") RegisterRequest request,
            @RequestPart("photo") MultipartFile photo,
            @RequestPart("descripteur") String descripteur) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request, photo, descripteursJson.lire(descripteur)));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Connexion", description = "Authentifie un utilisateur et retourne un token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

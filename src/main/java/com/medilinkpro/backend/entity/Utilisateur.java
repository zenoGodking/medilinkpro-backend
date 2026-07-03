package com.medilinkpro.backend.entity;

import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entite racine Utilisateur. Toute personne s'authentifiant sur MediLinkPro
 * (Patient, Medecin, Admin, Directeur, Secretaire) herite de cette classe.
 * Strategie d'heritage JOINED : chaque sous-type a sa propre table liee par id.
 * Implemente UserDetails pour s'integrer directement a Spring Security.
 */
@Entity
@Table(name = "utilisateurs")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@ToString(exclude = "motDePasse")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Utilisateur implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire")
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @JsonIgnore
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @NotNull(message = "Le role est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false, updatable = false, length = 30)
    private Role role;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @lombok.Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /**
     * Statut de validation du compte. Patient et Admin sont APPROUVE des l'inscription.
     * Medecin, Secretaire et Directeur restent EN_ATTENTE jusqu'a validation par un Admin
     * (voir AuthService.register et AdminService) : ils ne peuvent pas se connecter avant.
     */
    @lombok.Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_compte", length = 20)
    private StatutCompte statutCompte = StatutCompte.APPROUVE;

    /** Renseigne par l'Admin lorsqu'un compte professionnel est rejete. */
    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @CreationTimestamp
    @Column(name = "date_inscription", nullable = false, updatable = false)
    private LocalDateTime dateInscription;

    // ---- Implementation Spring Security UserDetails ----

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return actif;
    }
}

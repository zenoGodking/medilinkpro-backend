package com.medilinkpro.backend.config;

import com.medilinkpro.backend.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration centrale de Spring Security :
 * - API stateless (pas de session, authentification par JWT a chaque requete)
 * - Autorisations par role sur les endpoints metier
 * - Swagger UI et endpoints d'authentification ouverts publiquement
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/etablissements/public/**",
            "/api/campagnes/actives",
            "/uploads/**",
            "/ws-alertes/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Module Geolocalisation & Etablissements : lecture ouverte aux roles authentifies,
                        // creation/modification/suppression (dont photos) reservees au Directeur et a l'Admin
                        .requestMatchers("/api/medecins/recherche").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/etablissements/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/etablissements/**").hasAnyRole("DIRECTEUR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/etablissements/**").hasAnyRole("DIRECTEUR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/etablissements/**").hasAnyRole("DIRECTEUR", "ADMIN")

                        // Reconnaissance faciale d'urgence : recherche ouverte a tout utilisateur connecte
                        // (donnees essentielles), carnet complet en lecture seule pour le personnel de sante
                        // (le service verifie en plus que le compte est approuve), photo geree par le patient.
                        .requestMatchers(HttpMethod.POST, "/api/reconnaissance-faciale/recherche").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reconnaissance-faciale/patients/*/carnet")
                        .hasAnyRole("MEDECIN", "INFIRMIER")
                        .requestMatchers("/api/reconnaissance-faciale/moi/**").hasRole("PATIENT")
                        .requestMatchers("/api/reconnaissance-faciale/**").denyAll()

                        // Carte d'urgence (QR) : reservee aux utilisateurs connectes, le patient gere la sienne
                        .requestMatchers("/api/carte-urgence/moi/**", "/api/carte-urgence/moi").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/carte-urgence/*").authenticated()

                        // Espace directeur : ses etablissements et leurs patients (identite seulement)
                        .requestMatchers("/api/directeur/**").hasRole("DIRECTEUR")

                        // Administration
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Tableau de bord / statistiques : Directeur et Admin
                        .requestMatchers("/api/dashboard/**").hasAnyRole("DIRECTEUR", "ADMIN")

                        // Carnet medical (dossiers, consultations, ordonnances, analyses, fiche patient) :
                        // premier filtre par role ici, regles fines dans CarnetAccesService :
                        // - lecture : le patient pour lui-meme, tout medecin valide, l'administrateur ;
                        // - ecriture : medecin autorise par le patient ou ancien patient ;
                        // - declaration de deces : tout medecin valide, annulation par l'administrateur.
                        .requestMatchers(HttpMethod.POST, "/api/patients/*/deces").hasRole("MEDECIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/*/deces").hasRole("ADMIN")
                        .requestMatchers("/api/dossiers-medicaux/**", "/api/consultations/**", "/api/ordonnances/**",
                                "/api/resultats-analyses/**", "/api/patients/**", "/api/carnets/**", "/api/suivi/**")
                        .hasAnyRole("PATIENT", "MEDECIN", "ADMIN")

                        // Rendez-vous : Patient, Medecin, Admin (participants verifies dans le controleur)
                        .requestMatchers("/api/rendez-vous/**")
                        .hasAnyRole("PATIENT", "MEDECIN", "ADMIN")

                        .requestMatchers("/api/medecins/**").hasAnyRole("MEDECIN", "ADMIN", "DIRECTEUR")

                        // Alertes de soins a domicile : le patient envoie/annule/note ses propres alertes,
                        // l'infirmiere consulte les alertes actives, y repond ou s'en retracte
                        .requestMatchers(HttpMethod.POST, "/api/alertes/patients/**").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/alertes/patients/**").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/alertes/*/annuler").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/alertes/*/noter").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/alertes/actives").hasAnyRole("INFIRMIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/alertes/*/repondre").hasAnyRole("INFIRMIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/alertes/*/retracter").hasAnyRole("INFIRMIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/alertes/*/compte-rendu").hasAnyRole("INFIRMIER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/alertes/infirmiers/**").hasAnyRole("INFIRMIER", "ADMIN")
                        // Geolocalisation : l'infirmiere partage sa position, le patient suit celle de
                        // l'infirmiere en route (le service verifie qu'il s'agit bien de son alerte)
                        .requestMatchers(HttpMethod.PUT, "/api/alertes/infirmiers/moi/position").hasRole("INFIRMIER")
                        .requestMatchers(HttpMethod.GET, "/api/alertes/*/suivi").hasRole("PATIENT")

                        // Campagnes des etablissements : creation/lecture interne deja couvertes par les
                        // regles /api/etablissements/** ci-dessus ; desactivation/suppression reservees
                        // au Directeur et a l'Admin (la lecture publique passe par PUBLIC_ENDPOINTS)
                        .requestMatchers(HttpMethod.PATCH, "/api/campagnes/*/desactiver").hasAnyRole("DIRECTEUR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/campagnes/*").hasAnyRole("DIRECTEUR", "ADMIN")

                        // Integration Medecin-Etablissement : la reponse est ouverte a Medecin/Directeur/Admin,
                        // le service verifie ensuite que l'acteur est bien celui attendu selon l'initiateur
                        .requestMatchers(HttpMethod.PATCH, "/api/demandes-integration/*/repondre").hasAnyRole("MEDECIN", "DIRECTEUR", "ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

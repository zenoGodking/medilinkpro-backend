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
            "/api/urgence/**",
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

                        // Administration
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Tableau de bord / statistiques : Directeur et Admin
                        .requestMatchers("/api/dashboard/**").hasAnyRole("DIRECTEUR", "ADMIN")

                        // Dossiers medicaux : Patient, Medecin, Secretaire, Admin
                        .requestMatchers("/api/dossiers-medicaux/**")
                        .hasAnyRole("PATIENT", "MEDECIN", "SECRETAIRE", "ADMIN")

                        // Consultations et ordonnances : Medecin, Patient (lecture), Secretaire, Admin
                        .requestMatchers("/api/consultations/**", "/api/ordonnances/**")
                        .hasAnyRole("MEDECIN", "PATIENT", "SECRETAIRE", "ADMIN")

                        // Rendez-vous : Patient, Medecin, Secretaire, Admin
                        .requestMatchers("/api/rendez-vous/**")
                        .hasAnyRole("PATIENT", "MEDECIN", "SECRETAIRE", "ADMIN")

                        // Gestion des patients et medecins (CRUD complet) : Secretaire, Admin, Directeur
                        .requestMatchers("/api/patients/**").hasAnyRole("PATIENT", "MEDECIN", "SECRETAIRE", "ADMIN")
                        .requestMatchers("/api/medecins/**").hasAnyRole("MEDECIN", "SECRETAIRE", "ADMIN", "DIRECTEUR")

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

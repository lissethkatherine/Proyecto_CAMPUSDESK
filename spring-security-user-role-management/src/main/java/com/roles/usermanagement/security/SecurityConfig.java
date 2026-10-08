package com.roles.usermanagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Clase principal de configuración de Spring Security.
 * Define la cadena de filtros de seguridad, políticas de sesión sin estado,
 * soporte de CORS para el frontend local y protección de rutas con JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configuración de la cadena de filtros de seguridad HTTP.
     *
     * @param http El objeto HttpSecurity a configurar.
     * @return La cadena de filtros procesada.
     * @throws Exception Si ocurre un error de configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuración del origen de recursos cruzados (CORS) para permitir
     * la conexión desde aplicaciones frontend locales (React, Vite, Angular, etc.).
     *
     * @return La fuente de configuración CORS para la aplicación.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes locales permitidos para el frontend
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:4200",
                "http://localhost:8080",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Cabeceras permitidas
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));

        // Cabeceras expuestas a las peticiones del cliente
        configuration.setExposedHeaders(List.of("Authorization"));

        // Permitir envío de credenciales / cookies si aplica
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Define el administrador de autenticación de Spring Security.
     *
     * @param authenticationConfiguration La configuración de autenticación global.
     * @return El administrador de autenticación.
     * @throws Exception Si ocurre un error al obtener el administrador.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Codificador de contraseñas mediante algoritmo BCrypt.
     *
     * @return La instancia de BCryptPasswordEncoder.
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

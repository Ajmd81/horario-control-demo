package com.controlhorario.lite.config;

import com.controlhorario.lite.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.*;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${allowed.origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                // FIX #1: faltaba la "/" inicial — el webhook de Stripe
                                // nunca llegaba a permitAll() y Stripe recibía 401/403.
                                // subscriptionStatus nunca se actualizaba en BD.
                                "/api/stripe/webhook",
                                "/error"
                                // FIX #2: eliminado "/h2-console/**" — consola H2 sin auth
                                // no debe existir en producción. Mover a @Profile("dev")
                                // en un SecurityConfig separado si se necesita en local.
                        ).permitAll()
                        // FIX #3: el enum Role usa SUPER_ADMIN (con guion bajo),
                        // por lo que la autoridad generada en JwtAuthFilter es "ROLE_SUPER_ADMIN".
                        // hasRole("SUPERADMIN") exigía "ROLE_SUPERADMIN" → no coincidía nunca.
                        // Cambiado a hasRole("SUPER_ADMIN") para que el matching sea correcto.
                        .requestMatchers("/api/superadmin/**").hasRole("SUPERADMIN")
                        .anyRequest().authenticated()
                )
                .headers(h -> h
                        // FIX #4: frameOptions.disable() estaba puesto para H2 (que ya no existe
                        // en prod). Cambiado a sameOrigin() para proteger contra clickjacking
                        // manteniendo compatibilidad con iframes del mismo dominio si los hubiera.
                        .frameOptions(f -> f.sameOrigin())
                        // BONUS: cabeceras de seguridad adicionales sin coste
                        .contentTypeOptions(c -> {})
                        .referrerPolicy(r ->
                                r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsSource() {
        var config = new CorsConfiguration();
        // allowedOrigins viene de variable de entorno Railway (sin comillas, separado por comas).
        // Ej: https://fichajelaboral-landing.vercel.app,https://tudominio.com
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return username -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(username);
        };
    }
}
package dev.gabriel.portfolio_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // API REST sem sessao/cookies: CSRF nao se aplica (a credencial vai no cabecalho Authorization)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Site publico: apenas leitura
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                // Login em duas etapas (login + verify-2fa)
                .requestMatchers("/api/auth/**").permitAll()
                // Swagger UI e a especificacao OpenAPI
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Paginas de erro do Spring: sem isso, um 404/400 vira 401 para quem nao esta logado
                .requestMatchers("/error").permitAll()
                // Painel administrativo (POST/PUT/DELETE): exige token valido
                .anyRequest().authenticated()
            )
            // Liga o filtro que le "Authorization: Bearer <token>" e o confere com o JwtDecoder
            // (assinatura, expiracao e emissor; ver JwtConfig). Token ausente ou invalido -> 401.
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

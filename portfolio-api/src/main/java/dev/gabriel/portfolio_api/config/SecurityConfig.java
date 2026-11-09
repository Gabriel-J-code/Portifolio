package dev.gabriel.portfolio_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
            // API REST sem sessao/cookies: CSRF nao se aplica (a autenticacao sera via JWT)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Site publico: apenas leitura
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                // Login + 2FA
                .requestMatchers("/api/auth/**").permitAll()
                // Swagger (quando for adicionado)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Painel administrativo (POST/PUT/DELETE): exige autenticacao
                .anyRequest().authenticated()
            );
        // TODO: registrar aqui o filtro JWT quando o AcessoService/JwtService estiverem prontos
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

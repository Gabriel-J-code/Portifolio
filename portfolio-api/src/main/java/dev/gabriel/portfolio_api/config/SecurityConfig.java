package dev.gabriel.portfolio_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
                // Swagger UI e a especificacao OpenAPI
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Paginas de erro do Spring: sem isso, um 404/400 vira 401 para quem nao esta logado
                .requestMatchers("/error").permitAll()
                // Painel administrativo (POST/PUT/DELETE): exige autenticacao
                .anyRequest().authenticated()
            )
            // TEMPORARIO: HTTP Basic com o usuario de adminTemporario() abaixo,
            // so para testar o CRUD pelo Swagger enquanto o login com 2FA + JWT nao existe.
            // TODO: remover e registrar o filtro JWT quando o AcessoService/JwtService estiverem prontos.
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * TEMPORARIO (sai junto com o httpBasic): usuario admin em memoria.
     * Por que nao usar so spring.security.user.*? Quando existe um bean PasswordEncoder,
     * o Spring Boot trata aquela senha como se ja fosse um hash BCrypt, e o login nunca bate.
     * Aqui geramos o hash nos mesmos, a partir da senha em texto do .env.
     */
    @Bean
    public UserDetailsService adminTemporario(
            @Value("${ADMIN_USER:admin}") String usuario,
            @Value("${ADMIN_PASSWORD}") String senha,
            PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(usuario).password(encoder.encode(senha)).roles("ADMIN").build());
    }
}

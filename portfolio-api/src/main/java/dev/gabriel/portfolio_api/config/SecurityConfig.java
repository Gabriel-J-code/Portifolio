package dev.gabriel.portfolio_api.config;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Liga o filtro de CORS usando o bean corsConfigurationSource() abaixo.
            // Ele roda ANTES da autenticacao: o "preflight" (OPTIONS) do navegador nao leva token
            // e precisa ser respondido sem exigir login.
            .cors(Customizer.withDefaults())
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

    /**
     * CORS: o navegador so deixa um site (ex.: o Flutter em https://gabriel.dev) ler respostas
     * de OUTRO dominio (a API) se a API disser que aquele site e permitido.
     * Nao e uma protecao da API (curl/Postman ignoram CORS); protege o USUARIO, impedindo que
     * um site qualquer use o navegador dele para chamar a API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
        CorsConfiguration config = new CorsConfiguration();
        // Padroes (e nao origens exatas) para aceitar "http://localhost:[*]": o Flutter Web em dev usa porta aleatoria
        config.setAllowedOriginPatterns(cors.origensPermitidas());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Por padrao o navegador esconde a maioria dos cabecalhos da resposta; o painel precisa ler o Location do 201
        config.setExposedHeaders(List.of("Location"));
        // Sem cookies: o token vai no cabecalho Authorization, entao credenciais do navegador ficam desligadas
        config.setAllowCredentials(false);
        // O navegador guarda a resposta do preflight por 1h, evitando um OPTIONS antes de cada requisicao
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

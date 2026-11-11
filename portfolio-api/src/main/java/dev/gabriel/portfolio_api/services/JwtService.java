package dev.gabriel.portfolio_api.services;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import dev.gabriel.portfolio_api.config.AuthProperties;
import dev.gabriel.portfolio_api.config.JwtConfig;
import dev.gabriel.portfolio_api.dto.TokenResponse;

/**
 * Emite os tokens JWT. Quem CONFERE os tokens nao e este service: e o filtro do Spring Security
 * (oauth2ResourceServer no SecurityConfig), usando o JwtDecoder do JwtConfig.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, AuthProperties authProperties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    /**
     * Conteudo ("claims") do token:
     * - iss: quem emitiu (conferido na volta)
     * - sub: de quem e o token (o e-mail da conta)
     * - iat / exp: quando foi emitido e quando expira
     */
    public TokenResponse gerarToken(String email) {
        Instant agora = Instant.now(clock);
        Instant expiraEm = agora.plus(authProperties.tokenValidade());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISSOR)
                .subject(email)
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiraEm);
    }
}

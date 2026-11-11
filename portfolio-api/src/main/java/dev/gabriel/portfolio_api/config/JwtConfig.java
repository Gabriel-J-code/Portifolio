package dev.gabriel.portfolio_api.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Como os tokens sao assinados e conferidos.
 *
 * Algoritmo HS256 (HMAC-SHA256): a MESMA chave secreta assina e confere. Serve porque quem emite
 * e quem confere os tokens e a mesma aplicacao. (Com varios servicos, o comum e RS256: chave
 * privada para assinar e chave publica para conferir.)
 *
 * O token NAO e criptografado, so assinado: qualquer um le o conteudo (e Base64), mas ninguem
 * consegue altera-lo sem a chave. Por isso nao se coloca nada secreto dentro dele.
 */
@Configuration
public class JwtConfig {

    /** Quem emitiu o token ("iss"). O decoder recusa tokens com outro emissor. */
    public static final String EMISSOR = "portfolio-api";

    private final SecretKey chave;

    public JwtConfig(AuthProperties authProperties) {
        byte[] bytes = Base64.getDecoder().decode(authProperties.jwtSecret());
        if (bytes.length < 32) {
            // HS256 exige chave de no minimo 256 bits; chave curta = token mais facil de forjar
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 bytes (256 bits) em Base64");
        }
        this.chave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Usado pelo JwtService para GERAR tokens. */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /**
     * Usado pelo filtro do Spring Security para CONFERIR o token de cada requisicao:
     * assinatura, expiracao ("exp") e emissor ("iss").
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
        return decoder;
    }
}

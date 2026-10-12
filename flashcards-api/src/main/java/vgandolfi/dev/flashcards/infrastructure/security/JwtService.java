package vgandolfi.dev.flashcards.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import vgandolfi.dev.flashcards.domain.model.User;
import vgandolfi.dev.flashcards.infrastructure.config.AppProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Geracao e validacao de JWT (HS256) — stateless.
 *
 * <p>Access token: claims {@code sub} (userId), {@code email}, {@code name},
 * {@code token_type=ACCESS} e expiracao {@code app.jwt.expiration}. Refresh
 * token: claim {@code token_type=REFRESH} e expiracao
 * {@code app.jwt.refresh-expiration} (sem persistencia em banco).</p>
 */
@Service
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        String secret = appProperties.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret nao configurado. Defina a variavel JWT_SECRET (minimo 32 caracteres para HS256).");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getName())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(appProperties.jwt().expiration())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(appProperties.jwt().refreshExpiration())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Valida assinatura + expiracao e exige {@code token_type=ACCESS}. */
    public JwtClaims parseAccessToken(String token) {
        Claims claims = parseSignedClaims(token);
        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new MalformedJwtException("Token nao e do tipo ACCESS");
        }
        return new JwtClaims(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                claims.get(CLAIM_NAME, String.class),
                claims.getExpiration().toInstant());
    }

    /**
     * Valida assinatura + expiracao e exige {@code token_type=REFRESH}.
     *
     * @return userId (sub)
     */
    public UUID parseRefreshToken(String token) {
        Claims claims = parseSignedClaims(token);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new MalformedJwtException("Token nao e do tipo REFRESH");
        }
        return UUID.fromString(claims.getSubject());
    }

    private Claims parseSignedClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.apprh.backend.auth.infrastructure;

import com.apprh.backend.users.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String DEFAULT_SECRET = "dev-secret-a-changer-avant-production";
    /** HS256 exige au moins 256 bits (32 octets) de clé. */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Environment environment) {
        this.properties = properties;
        String secret = properties.secret() == null ? "" : properties.secret();
        if (secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret manquant ou vide : définissez la variable d'environnement JWT_SECRET "
                            + "(32 caractères minimum).");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret invalide : un minimum de " + MIN_SECRET_BYTES
                            + " octets est requis pour HS256.");
        }
        if (DEFAULT_SECRET.equals(secret)) {
            if (environment.matchesProfiles("prod")) {
                throw new IllegalStateException(
                        "JWT secret de développement interdit avec le profil « prod » : "
                                + "définissez un JWT_SECRET unique et robuste.");
            }
            log.warn("JWT_SECRET non défini : secret de développement utilisé (profil dev uniquement). "
                    + "Définissez un secret fort (32+ caractères) avant toute mise en production.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

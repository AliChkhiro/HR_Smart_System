package com.apprh.backend;

import com.apprh.backend.auth.infrastructure.JwtProperties;
import com.apprh.backend.auth.infrastructure.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie que le service JWT échoue explicitement lorsque le secret est absent,
 * vide, trop court, ou qu'il s'agit du secret de développement sous le profil prod.
 */
class JwtServiceTest {

    private static final String DEV_SECRET = "dev-secret-a-changer-avant-production";
    private static final String STRONG_SECRET = "un-secret-de-test-suffisamment-long-0123456789";

    private JwtProperties properties(String secret) {
        return new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7));
    }

    private JwtService service(String secret, String... activeProfiles) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(activeProfiles);
        return new JwtService(properties(secret), environment);
    }

    @Test
    void rejectsNullSecret() {
        assertThatThrownBy(() -> service(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret manquant");
    }

    @Test
    void rejectsBlankSecret() {
        assertThatThrownBy(() -> service(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret manquant");
        assertThatThrownBy(() -> service("    "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret manquant");
    }

    @Test
    void rejectsTooShortSecret() {
        assertThatThrownBy(() -> service("secret-trop-court"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret invalide");
    }

    @Test
    void rejectsDevelopmentSecretUnderProdProfile() {
        assertThatThrownBy(() -> service(DEV_SECRET, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }

    @Test
    void allowsDevelopmentSecretOutsideProdProfile() {
        assertThat(service(DEV_SECRET)).isNotNull();
    }

    @Test
    void acceptsStrongSecret() {
        JwtService jwtService = service(STRONG_SECRET, "prod");
        assertThat(jwtService.generateRefreshToken()).isNotBlank();
        assertThat(jwtService.sha256("valeur")).hasSize(64);
    }
}

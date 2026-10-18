package com.apprh.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verrouille l'invariant de sécurité C1 : aucun secret JWT de développement
 * ne doit servir de valeur de repli en dehors du profil dev, et la production
 * ne doit fournir aucune valeur par défaut pour JWT_SECRET ni DB_PASSWORD.
 */
class JwtProfileConfigurationTest {

    private static final String DEV_SECRET = "dev-secret-a-changer-avant-production";

    private PropertySource<?> load(String file) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load(file, new ClassPathResource(file));
        return sources.get(0);
    }

    @Test
    void baseConfigurationContainsNoJwtFallback() throws IOException {
        String yaml = new ClassPathResource("application.yml")
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).doesNotContain(DEV_SECRET);
        assertThat(load("application.yml").getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET:}");
        assertThat(load("application.yml").getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD:}");
    }

    @Test
    void productionProfileRequiresSecretsWithoutFallback() throws IOException {
        PropertySource<?> prod = load("application-prod.yml");
        assertThat(prod.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(String.valueOf(prod.getProperty("app.jwt.secret"))).doesNotContain("dev-secret");
        assertThat(prod.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(prod.getProperty("spring.datasource.url")).isEqualTo("${DB_URL}");
    }

    @Test
    void developmentProfileKeepsDocumentedDevOnlyFallback() throws IOException {
        PropertySource<?> dev = load("application-dev.yml");
        assertThat(dev.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET:" + DEV_SECRET + "}");
        assertThat(dev.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD:apprh}");
    }
}

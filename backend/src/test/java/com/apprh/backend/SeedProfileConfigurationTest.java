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
 * Verrouille l'invariant C2 : les comptes de démonstration ne sont jamais
 * activés par défaut ni en production, et le mot de passe de démonstration
 * n'apparaît que dans le profil de développement.
 */
class SeedProfileConfigurationTest {

    private static final String DEMO_PASSWORD = "Apprh@2026!";

    private PropertySource<?> load(String file) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load(file, new ClassPathResource(file));
        return sources.get(0);
    }

    @Test
    void baseConfigurationDisablesSeedingAndCarriesNoDemoPassword() throws IOException {
        assertThat(load("application.yml").getProperty("app.seed.enabled")).isEqualTo("${SEED_ENABLED:false}");
        assertThat(load("application.yml").getProperty("app.seed.password")).isEqualTo("${SEED_PASSWORD:}");
        String yaml = new ClassPathResource("application.yml").getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).doesNotContain(DEMO_PASSWORD);
    }

    @Test
    void developmentProfileEnablesSeedingWithDocumentedDemoPassword() throws IOException {
        PropertySource<?> dev = load("application-dev.yml");
        assertThat(dev.getProperty("app.seed.enabled")).isEqualTo("${SEED_ENABLED:true}");
        assertThat(dev.getProperty("app.seed.password")).isEqualTo("${SEED_PASSWORD:" + DEMO_PASSWORD + "}");
    }

    @Test
    void productionProfileNeverSeedsAndContainsNoDemoPassword() throws IOException {
        PropertySource<?> prod = load("application-prod.yml");
        assertThat(prod.getProperty("app.seed.enabled")).isEqualTo("${SEED_ENABLED:false}");
        assertThat(prod.getProperty("app.seed.password")).isNull();
        String yaml = new ClassPathResource("application-prod.yml").getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).doesNotContain(DEMO_PASSWORD);
    }
}

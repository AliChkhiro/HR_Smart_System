package com.apprh.backend;

import com.apprh.backend.common.config.DataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preuve d'exécution C2 : lorsque `app.seed.enabled=false`, le bean DataSeeder
 * n'est pas enregistré, donc aucun compte de démonstration n'est créé
 * (comportement du profil de production).
 */
@SpringBootTest(properties = "app.seed.enabled=false")
class SeedDisabledContextTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void seederIsNotRegisteredWhenSeedingIsDisabled() {
        assertThat(applicationContext.getBeanNamesForType(DataSeeder.class)).isEmpty();
    }
}

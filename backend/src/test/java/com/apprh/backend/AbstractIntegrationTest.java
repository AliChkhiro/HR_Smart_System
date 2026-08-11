package com.apprh.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
abstract class AbstractIntegrationTest {

    private static final class PostgresHolder {
        private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:17-alpine");

        static {
            INSTANCE.start();
        }
    }

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = PostgresHolder.INSTANCE;
}

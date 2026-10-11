package com.aac27.joyeria;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JoyeriaApplicationTests {

    private static final String MYSQL_BASE_URL = getEnv("AAC27_TEST_MYSQL_BASE_URL", "jdbc:mysql://localhost:3307");
    private static final String ROOT_USER = getEnv("AAC27_TEST_MYSQL_ROOT_USER", "root");
    private static final String ROOT_PASSWORD = getEnv("AAC27_TEST_MYSQL_ROOT_PASSWORD", "aac27_root_password");
    private static final String TEST_DATABASE = getEnv("AAC27_TEST_MYSQL_DATABASE", "aac27_joyeria_test");
    private static final String TEST_USER = getEnv("AAC27_TEST_MYSQL_USER", "aac27_test");
    private static final String TEST_PASSWORD = getEnv("AAC27_TEST_MYSQL_PASSWORD", "aac27_test_password");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        ensureTestDatabaseExists();

        String jdbcUrl = "%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota"
                .formatted(MYSQL_BASE_URL, TEST_DATABASE);

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> TEST_USER);
        registry.add("spring.datasource.password", () -> TEST_PASSWORD);
        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> TEST_USER);
        registry.add("spring.flyway.password", () -> TEST_PASSWORD);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void flywayAppliesVersionedMigrations() {
        MigrationInfo[] appliedMigrations = flyway.info().applied();

        assertThat(appliedMigrations)
                .extracting(MigrationInfo::getVersion)
                .extracting(Object::toString)
                .contains("9");

        assertThat(Arrays.stream(appliedMigrations))
                .noneMatch(migration -> migration.getState() == MigrationState.FAILED);
    }

    private static void ensureTestDatabaseExists() {
        try (Connection connection = DriverManager.getConnection(
                "%s/?useSSL=false&allowPublicKeyRetrieval=true".formatted(MYSQL_BASE_URL),
                ROOT_USER,
                ROOT_PASSWORD);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("DROP DATABASE IF EXISTS " + TEST_DATABASE);
            statement.executeUpdate("CREATE DATABASE " + TEST_DATABASE + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            statement.executeUpdate("CREATE USER IF NOT EXISTS '" + TEST_USER + "'@'%' IDENTIFIED BY '" + TEST_PASSWORD + "'");
            statement.executeUpdate("GRANT ALL PRIVILEGES ON " + TEST_DATABASE + ".* TO '" + TEST_USER + "'@'%'");
            statement.executeUpdate("FLUSH PRIVILEGES");
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "No se pudo preparar la base MySQL de pruebas. Verifica que docker compose tenga MySQL en localhost:3307.",
                    exception);
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

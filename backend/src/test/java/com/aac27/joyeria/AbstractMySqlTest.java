package com.aac27.joyeria;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Base para pruebas que necesitan la base MySQL de pruebas (aac27_joyeria_test). */
@ActiveProfiles("test")
public abstract class AbstractMySqlTest {

    private static final String MYSQL_BASE_URL = getEnv("AAC27_TEST_MYSQL_BASE_URL", "jdbc:mysql://localhost:3307");
    private static final String ROOT_USER = getEnv("AAC27_TEST_MYSQL_ROOT_USER", "root");
    private static final String ROOT_PASSWORD = getEnv("AAC27_TEST_MYSQL_ROOT_PASSWORD", "aac27_root_password");
    private static final String TEST_DATABASE = getEnv("AAC27_TEST_MYSQL_DATABASE", "aac27_joyeria_test");
    private static final String TEST_USER = getEnv("AAC27_TEST_MYSQL_USER", "aac27_test");
    private static final String TEST_PASSWORD = getEnv("AAC27_TEST_MYSQL_PASSWORD", "aac27_test_password");

    /** Evita recrear la base cada vez que se carga un contexto de Spring distinto. */
    private static boolean baseDeDatosLista = false;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        prepararBaseDeDatosUnaVez();

        String jdbcUrl = "%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota"
                .formatted(MYSQL_BASE_URL, TEST_DATABASE);

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> TEST_USER);
        registry.add("spring.datasource.password", () -> TEST_PASSWORD);
        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> TEST_USER);
        registry.add("spring.flyway.password", () -> TEST_PASSWORD);
    }

    private static synchronized void prepararBaseDeDatosUnaVez() {
        if (baseDeDatosLista) {
            return;
        }
        ensureTestDatabaseExists();
        baseDeDatosLista = true;
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
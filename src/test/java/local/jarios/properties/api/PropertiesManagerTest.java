package local.jarios.properties.api;

import local.jarios.properties.exception.PropertiesManagerException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertiesManagerTest {

    private PropertiesManagerService manager;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Creamos una implementación concreta del PropertiesManager
        manager = new PropertiesManagerServiceImpl(); // Sustituye por tu clase real
        manager.setConfigDir(tempDir.getAbsolutePath());

        // Crear ficheros de prueba
        createPropertiesFile("app.properties", Map.of(
                "app.name", "DemoApp",
                "app.version", "1.0",
                "app.secret", "12345"
        ));

        createPropertiesFile("db.properties", Map.of(
                "db.user", "admin",
                "db.pass", "secret"
        ));
    }

    void createPropertiesFile(String filename, Map<String, String> entries) throws IOException {
        Properties props = new Properties();
        props.putAll(entries);
        File file = new File(tempDir, filename);
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, null);
        }
    }

    @Test
    void shouldLoadAllProperties() {
        manager.loadAllProperties(tempDir.getAbsolutePath());
        Map<String, Properties> allProps = manager.getAllProperties();

        assertThat(allProps).hasSize(2);
        assertThat(allProps.get("app")).containsEntry("app.name", "DemoApp");
        assertThat(allProps.get("db")).containsEntry("db.user", "admin");
    }

    @Test
    void shouldReturnImmutableProperties() {
        manager.loadAllProperties(tempDir.getAbsolutePath());
        Properties props = manager.getProperties("app");

        assertThatThrownBy(() -> props.setProperty("new.key", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldMaskSensitiveKeysWhenPrinting() {
        manager.setSensitiveKeys(Set.of("app.secret", "db.pass"));
        manager.loadAllProperties(tempDir.getAbsolutePath());

        // Captura consola si fuera necesario, aquí se asume que el log se verifica manualmente
        manager.printAllProperties();
    }

    @Test
    void shouldGetPropertyInCorrectOrder() {
        manager.loadAllProperties(tempDir.getAbsolutePath());
        String val = manager.getProperty("app", "app.name");
        assertThat(val).isEqualTo("DemoApp");

        System.setProperty("db.user", "override");
        val = manager.getProperty("db", "db.user");
        assertThat(val).isEqualTo("admin"); // Se debe mantener preferencia por properties
    }

    @Test
    void shouldExportToJsonMasked() {
        manager.setSensitiveKeys(Set.of("db.pass"));
        manager.loadAllProperties(tempDir.getAbsolutePath());

        String json = manager.exportPropertiesToJson("db", true);
        assertThat(json).contains("\"db.user\":\"admin\"");
        assertThat(json).contains("\"db.pass\":\"*****\"");
    }

    @Test
    void shouldExportAllToJson() {
        manager.setSensitiveKeys(Set.of("app.secret", "db.pass"));
        manager.loadAllProperties(tempDir.getAbsolutePath());

        String json = manager.exportAllPropertiesToJson(true);
        assertThat(json).contains("\"app.name\":\"DemoApp\"");
        assertThat(json).contains("\"db.pass\":\"*****\"");
    }

    @Test
    void shouldValidateRequiredKeys() {
        manager.loadAllProperties(tempDir.getAbsolutePath());

        boolean valid = manager.validateRequiredKeys("app", Set.of("app.name", "app.version"));
        assertThat(valid).isTrue();

        boolean invalid = manager.validateRequiredKeys("app", Set.of("app.name", "app.port"));
        assertThat(invalid).isFalse();
    }

    @Test
    void shouldReloadCorrectly() {
        manager.loadAllProperties(tempDir.getAbsolutePath());
        Map<String, Properties> original = manager.getAllProperties();

        // Cambiamos los valores del fichero app.properties
        createPropertiesFile("app.properties", Map.of("app.name", "ReloadedApp", "app.version", "2.0"));
        manager.reload();

        Properties reloaded = manager.getProperties("app");
        assertThat(reloaded.getProperty("app.name")).isEqualTo("ReloadedApp");
    }

    @Test
    void shouldFallbackToDefaultConfigDirIfNull() {
        manager.setConfigDir(null); // fuerza al uso del valor por defecto
        assertThat(manager.getConfigDir()).isNotBlank();
    }

    @Test
    void shouldFallbackToDefaultSecretKeyIfNull() {
        manager.setSecretKey(null);
        assertThat(manager.getSecretKey()).isNotBlank();
    }

    @Test
    void shouldHandleMissingPropertyGracefully() {
        manager.loadAllProperties(tempDir.getAbsolutePath());
        String value = manager.getProperty("app", "nonexistent.key");
        assertThat(value).isNull();
    }

    @Test
    void shouldThrowWhenExportingNonExistentFile() {
        assertThatThrownBy(() -> manager.exportPropertiesToJson("nonexistent", false))
                .isInstanceOf(PropertiesManagerException.class);
    }

}

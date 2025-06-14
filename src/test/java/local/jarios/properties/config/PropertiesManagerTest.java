package local.jarios.property.config;

import local.jarios.property.exception.PropertiesLoadException;
import org.junit.jupiter.api.*;

import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertiesManagerTest {

    private static PropertiesManager manager;

    @BeforeAll
    static void setup() {
        manager = PropertiesManager.getInstance();
    }

    @Test
    @Order(1)
    void testSingletonInstance() {
        PropertiesManager another = PropertiesManager.getInstance();
        assertSame(manager, another, "Debe ser la misma instancia singleton");
    }

    @Test
    @Order(2)
    void testLoadPropertiesExist() {
        Properties props = manager.getProperties("app");
        assertNotNull(props, "El fichero app.properties debe existir");
        assertFalse(props.isEmpty(), "app.properties no debe estar vacío");
    }

    @Test
    @Order(3)
    void testGetPropertyFallback() {
        // Suponemos que 'JAVA_HOME' está en env o sistema
        String javaHome = manager.getProperty("nonexistentFile", "JAVA_HOME");
        assertNotNull(javaHome, "Debe obtener JAVA_HOME del sistema o entorno");
    }

    @Test
    @Order(4)
    void testPrintPropertiesException() {
        Exception e = assertThrows(PropertiesLoadException.class, () -> {
            manager.printProperties("noExiste");
        });
        assertTrue(e.getMessage().contains("No se encontró el fichero"));
    }

    @Test
    @Order(5)
    void testValidateRequiredKeys() {
        Set<String> required = Set.of("app.name");
        assertDoesNotThrow(() -> manager.validateRequiredKeys("app", required));
    }

    @Test
    @Order(6)
    void testValidateRequiredKeysFail() {
        Set<String> required = Set.of("clave.inexistente");
        PropertiesLoadException e = assertThrows(PropertiesLoadException.class, () -> {
            manager.validateRequiredKeys("app", required);
        });
        assertTrue(e.getMessage().contains("Clave requerida faltante"));
    }

    @Test
    @Order(7)
    void testExportAsJson() {
        String json = manager.exportAsJson("app", true);
        assertNotNull(json);
        assertTrue(json.contains("app.name"));
        assertTrue(json.contains("*****") || json.contains("app.name")); // máscara o valor real
    }

    @Test
    @Order(8)
    void testReload() {
        assertDoesNotThrow(() -> {
            manager.reload();
        });
    }
}

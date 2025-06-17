package local.jarios.properties.api;

import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.utils.Constantes;
import org.junit.jupiter.api.*;

import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertiesManagerTest {

    private static PropertiesManagerImpl manager;

    @BeforeAll
    static void setup() {
        manager = PropertiesManagerImpl.getInstance();
        manager.loadAllProperties(Constantes.CONFIG_DIR);
    }

    @Test
    @Order(1)
    void testSingletonInstance() {
        PropertiesManagerImpl another = PropertiesManagerImpl.getInstance();
        assertSame(manager, another, "Debe ser la misma instancia singleton");
    }

    @Test
    @Order(2)
    void testLoadPropertiesExist() {
        Properties props = manager.getProperties(Constantes.APP_PROPERTIES);
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
        PropertiesManagerException e = assertThrows(PropertiesManagerException.class, () -> {
            manager.printProperties("noExiste");
        });
        assertTrue(e.getMessage().contains("No se encontró el fichero"));
    }

    @Test
    @Order(5)
    void testValidateRequiredKeysSuccess() {
        Set<String> required = Set.of("app.name");
        boolean valid = manager.validateRequiredKeys(Constantes.APP_PROPERTIES, required);
        assertTrue(valid, "Todas las claves requeridas deben estar presentes");
    }

    @Test
    @Order(6)
    void testValidateRequiredKeysFail() {
        Set<String> required = Set.of("clave.inexistente");
        boolean valid = manager.validateRequiredKeys(Constantes.APP_PROPERTIES, required);
        assertFalse(valid, "Debe detectar que falta alguna clave requerida");
    }

    @Test
    @Order(7)
    void testExportPropertiesToJson() {
        String json = manager.exportPropertiesToJson(Constantes.APP_PROPERTIES, true);
        assertNotNull(json);
        assertTrue(json.contains("app.name"), "JSON exportado debe contener 'app.name'");

        // Verificamos que al menos contenga la máscara o el valor real
        assertTrue(json.contains("******") || json.contains("app.name"));
    }

    @Test
    @Order(8)
    void testReload() {
        assertDoesNotThrow(() -> {
            manager.reload();
        });
    }
}

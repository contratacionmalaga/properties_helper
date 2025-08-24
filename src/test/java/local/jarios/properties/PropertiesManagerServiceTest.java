package local.jarios.properties;

import local.jarios.properties.api.PropertiesManagerService;
import local.jarios.properties.api.PropertiesManagerServiceImpl;
import local.jarios.properties.common.util.Constantes;
import local.jarios.properties.exception.PropertiesManagerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite para PropertiesManagerService adaptada a la implementación actual.
 */
class PropertiesManagerServiceTest {

  private static PropertiesManagerService propertiesManager;

  @BeforeAll
  static void setup() throws PropertiesManagerException {
    propertiesManager = PropertiesManagerServiceImpl.getInstance();
    propertiesManager.setConfigDir(Constantes.PROPERTIES_DIR);

    Set<String> sensitiveKeys = new HashSet<>();
    sensitiveKeys.add("password");
    propertiesManager.setSensitiveKeys(sensitiveKeys);
  }

  @Test
  void testGetAndSetSensitiveKeys() throws PropertiesManagerException {
    Set<String> keys = new HashSet<>();
    keys.add("secretKey");
    propertiesManager.setSensitiveKeys(keys);

    Set<String> retrieved = propertiesManager.getSensitiveKeys();
    assertTrue(retrieved.contains("secretKey"));
  }

  @Test
  void testGetListFiles() throws PropertiesManagerException {
    List<String> files = propertiesManager.getListFiles();
    assertNotNull(files);
  }

  @Test
  void testLoadAllProperties() throws PropertiesManagerException {
    propertiesManager.loadAllProperties();
    Map<String, Properties> allProps = propertiesManager.getAllProperties();
    assertNotNull(allProps);
  }

  @Test
  void testPrintPropertiesAndPrintAllProperties() throws PropertiesManagerException {
    // Debe lanzar excepción si el archivo no existe
    assertThrows(PropertiesManagerException.class, () ->
        propertiesManager.printProperties("archivoInexistente"));

    // Solo se comprueba que no lanza excepción en un archivo cargado
    propertiesManager.loadAllProperties();
    List<String> files = propertiesManager.getListFiles();
    if (!files.isEmpty()) {
      assertDoesNotThrow(() -> propertiesManager.printProperties(files.getFirst()));
    }

    assertDoesNotThrow(propertiesManager::printAllProperties);
  }

  @Test
  void testGetProperties() throws PropertiesManagerException {
    propertiesManager.loadAllProperties();
    List<String> files = propertiesManager.getListFiles();
    if (!files.isEmpty()) {
      Properties props = propertiesManager.getProperties(files.getFirst());
      assertNotNull(props);
    }
  }

  @Test
  void testSetPropertyAndHasLoaded() throws PropertiesManagerException {
    String file = "testFile";
    String key = "testKey";
    String value = "123";

    propertiesManager.setProperty(file, key, value);
    assertTrue(propertiesManager.hasLoaded(file));
    assertEquals(value, propertiesManager.getProperty(file, key));
  }

  @Test
  void testGetPropertyThrowsExceptionForMissingKey() throws PropertiesManagerException {
    String file = "testFile";
    propertiesManager.addProperties(file, new Properties());

    assertThrows(PropertiesManagerException.class, () ->
        propertiesManager.getProperty(file, "nonexistentKey"));
  }

  @Test
  void testGetAllProperties() throws PropertiesManagerException {
    Map<String, Properties> allProps = propertiesManager.getAllProperties();
    assertNotNull(allProps);
  }

  @Test
  void testExportPropertiesToJson() throws PropertiesManagerException {
    propertiesManager.loadAllProperties();
    List<String> files = propertiesManager.getListFiles();
    if (!files.isEmpty()) {
      String file = files.getFirst();
      String json = propertiesManager.exportPropertiesToJson(file, true);
      assertNotNull(json);

      String allJson = propertiesManager.exportAllPropertiesToJson(true);
      assertNotNull(allJson);
    }

    // Archivo inexistente lanza excepción
    assertThrows(PropertiesManagerException.class, () ->
        propertiesManager.exportPropertiesToJson("archivoInexistente", true));
  }

  @Test
  void testValidateRequiredKeys() throws PropertiesManagerException {
    String file = "requiredTest";
    Properties props = new Properties();
    props.setProperty("key1", "val1");
    propertiesManager.addProperties(file, props);

    Set<String> keys = Set.of("key1");
    assertTrue(propertiesManager.validateRequiredKeys(file, keys));

    Set<String> missingKeys = Set.of("missingKey");
    assertFalse(propertiesManager.validateRequiredKeys(file, missingKeys));
  }

  @Test
  void testReload() {
    assertDoesNotThrow(() -> {
      try {
        propertiesManager.reload();
      } catch (PropertiesManagerException e) {
        fail("Reload lanzó excepción: " + e.getMessage());
      }
    });
  }

  @Test
  void testGetAndSetConfigDir() throws PropertiesManagerException {
    String currentDir = propertiesManager.getConfigDir();
    assertNotNull(currentDir);

    String testDir = Constantes.PROPERTIES_DIR;
    propertiesManager.setConfigDir(testDir);
    assertEquals(testDir, propertiesManager.getConfigDir());
  }

  @Test
  void testAddProperties() throws PropertiesManagerException {
    String file = "custom_test";
    Properties props = new Properties();
    props.setProperty("a", "1");
    props.setProperty("b", "2");

    propertiesManager.addProperties(file, props);
    Properties retrieved = propertiesManager.getProperties(file);
    assertEquals("1", retrieved.getProperty("a"));
    assertEquals("2", retrieved.getProperty("b"));
  }
}

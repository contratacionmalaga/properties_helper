package local.jarios.properties;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import local.jarios.properties.api.PropertiesManagerService;
import local.jarios.properties.api.PropertiesManagerServiceImpl;
import local.jarios.properties.common.util.Constantes;
import local.jarios.properties.common.util.Mensajes;
import local.jarios.properties.enums.TipoFinalEjecucion;
import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.helpers.FinalDelProgramaHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase de demostración para mostrar el uso básico del servicio PropertiesManagerService.
 *
 * <p>Esta clase contiene el método main que inicializa el servicio, carga propiedades y realiza
 * algunas operaciones de ejemplo, registrando información a través del log.
 *
 * @author Juan Antonio
 * @version 1.0
 * @since 2024-06-17
 */
@Slf4j
public class PropertiesDemo {

  /** Instancia singleton del servicio de gestión de propiedades. */
  static PropertiesManagerService propertiesManager = null;

  /** Constructor vacío. */
  private PropertiesDemo() {
    // Constructor vacío
  }

  /**
   * Método principal para ejecutar la demo del PropertiesManager.
   *
   * @param args Argumentos de línea de comandos (no se usan)
   */
  public static void main(String[] args) {

    // Inicio del log
    log.info(Mensajes.INICIO);

    propertiesManager = PropertiesManagerServiceImpl.getInstance();
    log.info("Obtenida la instancia de PropertiesManagerServiceImpl correctamente.");

    try {

      runAllTests();

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.CORRECTO);

    } catch (PropertiesManagerException ex) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR, ex.getMessage());
    }
  }

  /** Ejecuta todas las pruebas de demostración. */
  private static void runAllTests() {
    log.info("=== INICIANDO DEMO DEL PROPERTIES MANAGER ===");

    try {

      // Configuración inicial
      testConfigurationMethods();

      // Añadir propiedades y listar ficheros
      testAddPropertiesAndListFiles();

      // Carga de propiedades de prueba
      loadTestProperties();

      // Obtención de propiedades
      testPropertyRetrieval();

      // Impresión de propiedades
      testPropertyPrinting();

      // Exportación a JSON
      testJsonExport();

      // Validación de claves requeridas
      testKeyValidation();

      // Recarga de propiedades
      testReload();

      // Casos de error
      testErrorCases(); // <-- Aquí la llamada que faltaba

      // Prueba hasLoaded y setProperty
      testHasLoadedAndSetProperty();

      log.info("=== DEMO COMPLETADO EXITOSAMENTE ===");

    } catch (Exception ex) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR, ex.getMessage());
    }
  }

  /** Prueba métodos de configuración del directorio y clave secreta. */
  private static void testConfigurationMethods() {
    log.info("--- TEST - Probando métodos de configuración ---");

    try {
      // Configurar directorio
      propertiesManager.setConfigDir(Constantes.PROPERTIES_DIR);
      log.info(
          "Establecido el directorio de los ficheros properties: {}", Constantes.PROPERTIES_DIR);

      // Obtener directorio actual
      String currentDir = propertiesManager.getConfigDir();
      log.info("Recuperando el directorio de los ficheros properties: {}", currentDir);

      // Almacenaje de las claves sensibles
      Set<String> sensitiveKeys = new HashSet<>();
      sensitiveKeys.add("password");
      log.info("Estableciendo el conjunto de claves sensibles: {}", sensitiveKeys);
      propertiesManager.setSensitiveKeys(sensitiveKeys);

      // Obtención de las claves sensibles una vez almacenadaas
      log.info(
          "Recuperando el conjunto de claves sensibles: {}", propertiesManager.getSensitiveKeys());

    } catch (PropertiesManagerException ex) {

      logException("testConfigurationMethods", ex);
    }
  }

  /** Prueba la funcionalidad de añadir propiedades en memoria y listar ficheros cargados. */
  private static void testAddPropertiesAndListFiles() {
    log.info("--- TEST - Probando adición de propiedades y listado de ficheros ---");

    try {
      // Crear un nuevo conjunto de propiedades
      Properties customProps = new Properties();
      customProps.setProperty("custom.key1", "value1");
      customProps.setProperty("custom.key2", "value2");

      String customFileName = "custom_config";

      // Añadir propiedades al PropertiesManager
      propertiesManager.addProperties(customFileName, customProps);
      log.info("Añadidas propiedades al fichero '{}'", customFileName);

      // Verificar que las propiedades están disponibles
      Properties retrievedProps = propertiesManager.getProperties(customFileName);
      log.info("Propiedades recuperadas de '{}': {}", customFileName, retrievedProps);

      // Obtener listado de ficheros cargados
      List<String> loadedFiles = propertiesManager.getListFiles();
      log.info("Ficheros cargados actualmente: {}", loadedFiles);

      // Validar que el nuevo fichero aparece en el listado
      if (loadedFiles.contains(customFileName)) {
        log.info("El fichero '{}' está presente en el listado de ficheros", customFileName);
      } else {
        log.warn("El fichero '{}' NO está presente en el listado de ficheros", customFileName);
      }

    } catch (PropertiesManagerException ex) {

      logException("testAddPropertiesAndListFiles", ex);
    }
  }

  /** Carga propiedades de prueba en memoria. */
  private static void loadTestProperties() {
    log.info(
        "--- TEST - Cargando propiedades desde los ficheros ubicados en /{}",
        Constantes.PROPERTIES_DIR);

    try {

      propertiesManager.printAllProperties();
      propertiesManager.loadAllProperties();
      propertiesManager.printAllProperties();

    } catch (PropertiesManagerException ex) {

      logException("loadTestProperties", ex);
    }
  }

  /** Prueba la obtención de propiedades individuales y conjuntos completos. */
  private static void testPropertyRetrieval() {
    log.info("--- TEST - Probando obtención de propiedades ---");

    try {
      // Obtener propiedades individuales
      String appName =
          propertiesManager.getProperty(Constantes.APP_PROPERTIES, Constantes.KEY_APP_NAME);
      log.info("app.name = {}", appName);

      String nonExistent =
          propertiesManager.getProperty(Constantes.APP_PROPERTIES, Constantes.KEY_NON_EXISTS);
      log.info("Clave inexistente = {}", nonExistent);

      // Obtener propiedades de un archivo completo
      Properties appProperties = propertiesManager.getProperties(Constantes.APP_PROPERTIES);
      log.info(
          "Propiedades de '{}' obtenidas: {} elementos",
          Constantes.APP_PROPERTIES,
          appProperties.size());

      // Obtener todas las propiedades
      Map<String, Properties> allProps = propertiesManager.getAllProperties();
      log.info("Total de archivos cargados: {}", allProps.size());
      allProps
          .keySet()
          .forEach(
              fileName ->
                  log.info("  - {}: {} propiedades", fileName, allProps.get(fileName).size()));

    } catch (PropertiesManagerException ex) {

      logException("testPropertyRetrieval", ex);
    }
  }

  /** Prueba la impresión de propiedades en logs. */
  private static void testPropertyPrinting() {
    log.info("--- TEST - Probando impresión de propiedades ---");

    try {
      // Imprimir propiedades de un archivo específico
      log.info("Imprimiendo propiedades de '{}':", Constantes.EMAIL_PROPERTIES);
      propertiesManager.printProperties(Constantes.EMAIL_PROPERTIES);

      log.info(
          "Imprimiendo propiedades de '{}' (con valores sensibles enmascarados)",
          Constantes.APP_PROPERTIES);
      propertiesManager.printProperties(Constantes.APP_PROPERTIES);

      // Imprimir todas las propiedades
      log.info("Imprimiendo TODAS las propiedades:");
      propertiesManager.printAllProperties();

    } catch (PropertiesManagerException ex) {

      logException("testPropertyPrinting", ex);
    }
  }

  /** Prueba la exportación a formato JSON. */
  private static void testJsonExport() {
    log.info("--- TEST - Probando exportación a JSON ---");

    try {
      // Exportar un archivo específico sin enmascaramiento
      String appJson = propertiesManager.exportPropertiesToJson(Constantes.APP_PROPERTIES, false);
      log.info("JSON de '{}' (sin enmascarar)", Constantes.APP_PROPERTIES);
      log.info(appJson);

      // Exportar un archivo específico con enmascaramiento
      String dbJson = propertiesManager.exportPropertiesToJson(Constantes.EMAIL_PROPERTIES, true);
      log.info("JSON de '{}' (con enmascaramiento)", Constantes.EMAIL_PROPERTIES);
      log.info(dbJson);

      // Exportar todas las propiedades con enmascaramiento
      String allJson = propertiesManager.exportAllPropertiesToJson(true);
      log.info("JSON de TODAS las propiedades (con enmascaramiento)");
      log.info(allJson);

    } catch (PropertiesManagerException ex) {

      logException("testJsonExport", ex);
    }
  }

  /** Prueba la validación de claves requeridas. */
  private static void testKeyValidation() {
    log.info("--- TEST - Probando validación de claves requeridas ---");

    try {
      // Validar claves requeridas que existen
      Set<String> requiredAppKeys = Set.of(Constantes.KEY_APP_NAME);
      boolean validApp =
          propertiesManager.validateRequiredKeys(Constantes.APP_PROPERTIES, requiredAppKeys);
      log.info("Validación de '{}' con claves requeridas: {}", Constantes.APP_PROPERTIES, validApp);

      // Validar claves requeridas que no existen
      Set<String> requiredMissingKeys = Set.of(Constantes.KEY_NON_EXISTS);
      boolean validMissing =
          propertiesManager.validateRequiredKeys(Constantes.APP_PROPERTIES, requiredMissingKeys);
      log.info(
          "Validación de '{}' con claves faltantes: {}", Constantes.APP_PROPERTIES, validMissing);

      // Validar archivo inexistente
      try {
        boolean validNonExistent =
            propertiesManager.validateRequiredKeys(
                Constantes.APP_NON_EXISTS_PROPERTIES, requiredAppKeys);
        log.info("Validación de archivo inexistente: {}", validNonExistent);
      } catch (PropertiesManagerException e) {
        log.error(e.getMessage());
        FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
      }

    } catch (PropertiesManagerException ex) {

      logException("testKeyValidation", ex);
    }
  }

  /** Prueba la funcionalidad de recarga. */
  private static void testReload() {
    log.info("--- TEST - Probando recarga de propiedades ---");

    try {
      log.info("Ejecutando recarga...");
      propertiesManager.reload();
      log.info("Recarga completada exitosamente");

      // Verificar que las propiedades siguen disponibles después de la recarga
      Map<String, Properties> reloadedProps = propertiesManager.getAllProperties();
      log.info("Archivos disponibles después de la recarga: {}", reloadedProps.size());

    } catch (PropertiesManagerException ex) {

      logException("testReload", ex);
    }
  }

  /** Prueba casos de error y manejo de excepciones. */
  private static void testErrorCases() {
    log.info("--- TEST - Probando casos de error ---");

    // Intentar acceder a archivo inexistente
    try {

      String prop = propertiesManager.getProperty("archivo_inexistente", "clave");
      String msg = "Probando archivo inexistente. ";
      msg = (prop == null) ? msg + "Valor devuelto: null." : msg + "Valor devuelto: " + prop;
      log.info(msg);

    } catch (PropertiesManagerException ex) {

      logException("testErrorCases", ex);
    }

    // Intentar usar nombre de archivo inválido
    try {

      log.info("Probando a imprimir las properties de un fichero que no exsite.");
      propertiesManager.printProperties("");
      log.info("Se esperaba una excepción para nombre vacío");

    } catch (PropertiesManagerException ex) {

      logException("testErrorCases", ex);
    }

    // Intentar configurar directorio inválido
    try {
      propertiesManager.setConfigDir("/ruta/inexistente/completamente");
    } catch (PropertiesManagerException ex) {

      logException("testErrorCases", ex);
    }
  }

  /** Prueba la funcionalidad de hasLoaded y setProperty. */
  private static void testHasLoadedAndSetProperty() {
    log.info("--- TEST - Probando hasLoaded y setProperty ---");

    String testFile = "app.properties";
    String testKey = "test.key";
    String testValue = "value123";

    try {

      // 1. Establecer una propiedad en memoria
      propertiesManager.setProperty(testFile, testKey, testValue);
      log.info("Propiedad '{}'='{}' añadida en '{}'", testKey, testValue, testFile);

      // 2. Comprobar que el fichero ahora aparece como cargado
      boolean loaded = propertiesManager.hasLoaded(testFile);
      if (loaded) {
        log.info("El fichero '{}' se encuentra cargado en memoria.", testFile);
      } else {
        log.warn("El fichero '{}' NO se encuentra cargado en memoria.", testFile);
      }

      // 3. Verificar que el valor es correcto
      String valorRecuperado = propertiesManager.getProperty(testFile, testKey);
      log.info("Valor recuperado para '{}': {}", testKey, valorRecuperado);

    } catch (PropertiesManagerException ex) {

      logException("testHasLoadedAndSetProperty", ex);
    }
  }

  private static void logException(String context, PropertiesManagerException ex) {
    log.warn("Error en {}: {}", context, ex.getMessage());
  }
}

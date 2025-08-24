package local.jarios.properties;

import local.jarios.properties.api.PropertiesManagerService;
import local.jarios.properties.api.PropertiesManagerServiceImpl;
import local.jarios.properties.common.util.Constantes;
import local.jarios.properties.common.util.Mensajes;
import local.jarios.properties.enums.TipoFinalEjecucion;
import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.helpers.FinalDelProgramaHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Clase de demostración para mostrar el uso básico del servicio PropertiesManagerService.
 *
 * <p>Esta clase contiene el método main que inicializa el servicio,
 * carga propiedades y realiza algunas operaciones de ejemplo, registrando información a través del
 * LOGGER.</p>
 *
 * @author Juan Antonio
 * @version 1.0
 * @since 2024-06-17
 */
public class PropertiesDemo {

  /**
   * LOGGER del componente.
   */
  private static final Logger LOGGER = LogManager.getLogger("local.jarios.properties");

  /**
   * Instancia singleton del servicio de gestión de propiedades.
   */
  static PropertiesManagerService propertiesManager = null;

  /**
   * Constructor vacío.
   */
  private PropertiesDemo() {
    // Constructor vacío
  }

  /**
   * Ejecuta todas las pruebas de demostración.
   */
  private static void runAllTests() {
    LOGGER.info("=== INICIANDO DEMO DEL PROPERTIES MANAGER ===");

    try {
      // 1. Configuración inicial
      testConfigurationMethods();

      // 2. Carga de propiedades de prueba
      loadTestProperties();

      // 3. Obtención de propiedades
      testPropertyRetrieval();

      // 4. Impresión de propiedades
      testPropertyPrinting();

      // 5. Exportación a JSON
      testJsonExport();

      // 6. Validación de claves requeridas
      testKeyValidation();

      // 7. Recarga de propiedades
      testReload();

      // 8. Añadir propiedades y listar ficheros
      testAddPropertiesAndListFiles();

      // 9. Casos de error
      testErrorCases(); // <-- Aquí la llamada que faltaba

      // 10. Prueba hasLoaded y setProperty
      testHasLoadedAndSetProperty();

      LOGGER.info("=== DEMO COMPLETADO EXITOSAMENTE ===");

    } catch (Exception ex) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba métodos de configuración del directorio y clave secreta.
   */
  private static void testConfigurationMethods() {
    LOGGER.info("--- Probando métodos de configuración ---");

    try {
      // Configurar directorio
      propertiesManager.setConfigDir(Constantes.PROPERTIES_DIR);
      LOGGER.info("Establecido el directorio de los ficheros properties: {}",
                  Constantes.PROPERTIES_DIR);

      // Obtener directorio actual
      String currentDir = propertiesManager.getConfigDir();
      LOGGER.info("Recuperando el directorio de los ficheros properties: {}", currentDir);

      // Almacenaje de las claves sensibles
      Set<String> sensitiveKeys = new HashSet<>();
      sensitiveKeys.add("password");
      LOGGER.info("Estableciendo el conjunto de claves sensibles: {}", sensitiveKeys);
      propertiesManager.setSensitiveKeys(sensitiveKeys);

      // Obtención de las claves sensibles una vez almacenadaas
      LOGGER.info("Recuperando el conjunto de claves sensibles: {}",
                  propertiesManager.getSensitiveKeys());


    } catch (PropertiesManagerException ex) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba la funcionalidad de añadir propiedades en memoria y listar ficheros cargados.
   */
  private static void testAddPropertiesAndListFiles() {
    LOGGER.info("--- Probando adición de propiedades y listado de ficheros ---");

    try {
      // Crear un nuevo conjunto de propiedades
      Properties customProps = new Properties();
      customProps.setProperty("custom.key1", "value1");
      customProps.setProperty("custom.key2", "value2");

      String customFileName = "custom_config";

      // Añadir propiedades al PropertiesManager
      propertiesManager.addProperties(customFileName, customProps);
      LOGGER.info("Añadidas propiedades al fichero '{}'", customFileName);

      // Verificar que las propiedades están disponibles
      Properties retrievedProps = propertiesManager.getProperties(customFileName);
      LOGGER.info(
          "Propiedades recuperadas de '{}': {}", customFileName, retrievedProps);

      // Obtener listado de ficheros cargados
      List<String> loadedFiles = propertiesManager.getListFiles();
      LOGGER.info(
          "Ficheros cargados actualmente: {}", loadedFiles);

      // Validar que el nuevo fichero aparece en el listado
      if (loadedFiles.contains(customFileName)) {
        LOGGER.info(
            "El fichero '{}' está presente en el listado de ficheros", customFileName);
      } else {
        LOGGER.warn(
            "El fichero '{}' NO está presente en el listado de ficheros", customFileName);
      }

    } catch (PropertiesManagerException ex) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Carga propiedades de prueba en memoria.
   */
  private static void loadTestProperties() {
    LOGGER.info(
        "Cargando propiedades desde los ficheros ubicados en /{}",
        Constantes.PROPERTIES_DIR);

    try {

      propertiesManager.loadAllProperties();
      propertiesManager.printAllProperties();

    } catch (PropertiesManagerException e) {

      e.getStackTrace();
      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba la obtención de propiedades individuales y conjuntos completos.
   */
  private static void testPropertyRetrieval() {
    LOGGER.info("--- Probando obtención de propiedades ---");

    try {
      // Obtener propiedades individuales
      String appName = propertiesManager.getProperty(Constantes.APP_PROPERTIES,
                                                     Constantes.KEY_APP_NAME);
      LOGGER.info("app.name = {}", appName);

      String nonExistent = propertiesManager.getProperty(Constantes.APP_PROPERTIES,
                                                         Constantes.KEY_NON_EXISTS);
      LOGGER.info("Clave inexistente = {}", nonExistent);

      // Obtener propiedades de un archivo completo
      Properties appProperties = propertiesManager.getProperties(Constantes.APP_PROPERTIES);
      LOGGER.info("Propiedades de '{}' obtenidas: {} elementos", Constantes.APP_PROPERTIES,
                  appProperties.size());

      // Obtener todas las propiedades
      Map<String, Properties> allProps = propertiesManager.getAllProperties();
      LOGGER.info("Total de archivos cargados: {}", allProps.size());
      allProps.keySet().forEach(
          fileName ->
              LOGGER.info(
                  "  - {}: {} propiedades",
                  fileName,
                  allProps.get(fileName).size()));

    } catch (PropertiesManagerException e) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba la impresión de propiedades en logs.
   */
  private static void testPropertyPrinting() {
    LOGGER.info("--- Probando impresión de propiedades ---");

    try {
      // Imprimir propiedades de un archivo específico
      LOGGER.info("Imprimiendo propiedades de '{}':", Constantes.EMAIL_PROPERTIES);
      propertiesManager.printProperties(Constantes.EMAIL_PROPERTIES);

      LOGGER.info("Imprimiendo propiedades de '{}' (con valores sensibles enmascarados)",
                  Constantes.APP_PROPERTIES);
      propertiesManager.printProperties(Constantes.APP_PROPERTIES);

      // Imprimir todas las propiedades
      LOGGER.info("Imprimiendo TODAS las propiedades:");
      propertiesManager.printAllProperties();

    } catch (PropertiesManagerException e) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba la exportación a formato JSON.
   */
  private static void testJsonExport() {
    LOGGER.info("--- Probando exportación a JSON ---");

    try {
      // Exportar un archivo específico sin enmascaramiento
      String appJson = propertiesManager
          .exportPropertiesToJson(Constantes.APP_PROPERTIES, false);
      LOGGER.info("JSON de '{}' (sin enmascarar)", Constantes.APP_PROPERTIES);
      LOGGER.info(appJson);

      // Exportar un archivo específico con enmascaramiento
      String dbJson = propertiesManager
          .exportPropertiesToJson(Constantes.EMAIL_PROPERTIES, true);
      LOGGER.info("JSON de '{}' (con enmascaramiento)", Constantes.EMAIL_PROPERTIES);
      LOGGER.info(dbJson);

      // Exportar todas las propiedades con enmascaramiento
      String allJson = propertiesManager
          .exportAllPropertiesToJson(true);
      LOGGER.info("JSON de TODAS las propiedades (con enmascaramiento)");
      LOGGER.info(allJson);

    } catch (PropertiesManagerException e) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba la validación de claves requeridas.
   */
  private static void testKeyValidation() {
    LOGGER.info("--- Probando validación de claves requeridas ---");

    try {
      // Validar claves requeridas que existen
      Set<String> requiredAppKeys = Set.of(Constantes.KEY_APP_NAME);
      boolean validApp = propertiesManager.validateRequiredKeys(Constantes.APP_PROPERTIES,
                                                                requiredAppKeys);
      LOGGER.info("Validación de '{}' con claves requeridas: {}", Constantes.APP_PROPERTIES,
                  validApp);

      // Validar claves requeridas que no existen
      Set<String> requiredMissingKeys = Set.of(Constantes.KEY_NON_EXISTS);
      boolean validMissing = propertiesManager.validateRequiredKeys(Constantes.APP_PROPERTIES,
                                                                    requiredMissingKeys);
      LOGGER.info("Validación de '{}' con claves faltantes: {}", Constantes.APP_PROPERTIES,
                  validMissing);

      // Validar archivo inexistente
      try {
        boolean validNonExistent = propertiesManager.validateRequiredKeys(
            Constantes.APP_NON_EXISTS_PROPERTIES, requiredAppKeys);
        LOGGER.info("Validación de archivo inexistente: {}", validNonExistent);
      } catch (PropertiesManagerException e) {
        LOGGER.error(e.getMessage());
        FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
      }

    } catch (PropertiesManagerException e) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba la funcionalidad de recarga.
   */
  private static void testReload() {
    LOGGER.info("--- Probando recarga de propiedades ---");

    try {
      LOGGER.info("Ejecutando recarga...");
      propertiesManager.reload();
      LOGGER.info("Recarga completada exitosamente");

      // Verificar que las propiedades siguen disponibles después de la recarga
      Map<String, Properties> reloadedProps = propertiesManager.getAllProperties();
      LOGGER.info("Archivos disponibles después de la recarga: {}", reloadedProps.size());

    } catch (PropertiesManagerException e) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Prueba casos de error y manejo de excepciones.
   */
  private static void testErrorCases() {
    LOGGER.info("--- Probando casos de error ---");

    // Intentar acceder a archivo inexistente
    try {
      String prop = propertiesManager.getProperty(
          "archivo_inexistente", "clave");
      String msg = "Probando archivo inexistente. ";
      msg = (prop == null) ? msg + "Valor devuelto: null." : msg + "Valor devuelto: " + prop;
      LOGGER.info(msg);
    } catch (PropertiesManagerException e) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }

    // Intentar usar nombre de archivo inválido
    try {
      LOGGER.info("Probando a imprimir las properties de un fichero que no exsite.");
      propertiesManager.printProperties("");
      LOGGER.info("Se esperaba una excepción para nombre vacío");
    } catch (PropertiesManagerException ex) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }

    // Intentar configurar directorio inválido
    try {
      propertiesManager.setConfigDir("/ruta/inexistente/completamente");
    } catch (PropertiesManagerException e) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }

  /**
   * Método principal para ejecutar la demo del PropertiesManager.
   *
   * @param args Argumentos de línea de comandos (no se usan)
   */
  public static void main(String[] args) {

    // Inicio del log
    LOGGER.info(Mensajes.INICIO);

    propertiesManager = PropertiesManagerServiceImpl.getInstance();
    LOGGER.info("Obtenida la instancia de PropertiesManagerServiceImpl correctamente.");

    try {

      runAllTests();

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.CORRECTO);

    } catch (PropertiesManagerException ex) {

      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);

    }
  }

  /**
   * Prueba la funcionalidad de hasLoaded y setProperty.
   */
  private static void testHasLoadedAndSetProperty() {
    LOGGER.info("--- Probando hasLoaded y setProperty ---");

    String testFile = "app.properties";
    String testKey = "test.key";
    String testValue = "value123";

    try {
      // 1. Establecer una propiedad en memoria
      propertiesManager.setProperty(testFile, testKey, testValue);
      LOGGER.info("Propiedad '{}'='{}' añadida en '{}'", testKey, testValue, testFile);

      // 2. Comprobar que el fichero ahora aparece como cargado
      boolean loaded = propertiesManager.hasLoaded(testFile);
      if (loaded) {
        LOGGER.info("El fichero '{}' se encuentra cargado en memoria.", testFile);
      } else {
        LOGGER.warn("El fichero '{}' NO se encuentra cargado en memoria.", testFile);
      }

      // 3. Verificar que el valor es correcto
      String valorRecuperado = propertiesManager.getProperty(testFile, testKey);
      LOGGER.info("Valor recuperado para '{}': {}", testKey, valorRecuperado);

    } catch (PropertiesManagerException ex) {
      LOGGER.error("Error probando hasLoaded o setProperty: {}", ex.getMessage(), ex);
      FinalDelProgramaHelper.finalizar(TipoFinalEjecucion.ERROR);
    }
  }
}

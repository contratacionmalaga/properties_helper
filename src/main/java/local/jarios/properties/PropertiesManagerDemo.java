package local.jarios.properties;

import local.jarios.properties.api.PropertiesManagerService;
import local.jarios.properties.api.PropertiesManagerServiceImpl;
import local.jarios.properties.common.util.Constantes;
import local.jarios.properties.common.util.FinalDelPrograma;
import local.jarios.properties.enums.TipoFinalEjecucion;
import local.jarios.properties.exception.PropertiesManagerException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Clase de demostración para mostrar el uso básico del servicio
 * PropertiesManagerService.
 * <p>
 * Esta clase contiene el método main que inicializa el servicio,
 * carga propiedades y realiza algunas operaciones de ejemplo,
 * registrando información a través del log.
 * </p>
 *
 * @author Juan Antonio
 * @version 1.0
 * @since 2024-06-17
 */
@Slf4j
public class PropertiesManagerDemo {

    /**
     * Instancia singleton del servicio de gestión de propiedades.
     */
    static PropertiesManagerService propertiesManager = PropertiesManagerServiceImpl.getInstance();

    /**
     * Constructor vacío
     */
    private PropertiesManagerDemo() {
        // Constructor vacío
    }

    /**
     * Ejecuta todas las pruebas de demostración.
     */
    private static void runAllTests() {
        log.info("=== INICIANDO DEMO DEL PROPERTIES MANAGER ===");

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

            log.info("=== DEMO COMPLETADO EXITOSAMENTE ===");

        } catch (Exception e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Prueba métodos de configuración del directorio y clave secreta.
     */
    private static void testConfigurationMethods() {
        log.info("--- Probando métodos de configuración ---");

        try {
            // Configurar directorio
            propertiesManager.setConfigDir(Constantes.DEFAULT_CONFIG_DIR);
            log.info("Directorio configurado: {}", Constantes.DEFAULT_CONFIG_DIR);

            // Obtener directorio actual
            String currentDir = propertiesManager.getConfigDir();
            log.info("Directorio actual: {}", currentDir);

            // Configurar clave secreta
            propertiesManager.setSecretKey(Constantes.DEFAULT_SECRET_KEY);
            log.info("Clave secreta configurada");

            // Obtener clave secreta (se debería mostrar enmascarada)
            String currentKey = propertiesManager.getSecretKey();
            log.info("Clave secreta actual: {}", maskString(currentKey));

            // Almacenaje de las claves sensibles
            Set<String> sensitiveKeys = new HashSet<>();
            sensitiveKeys.add("password");
            log.info("Conjunto de claves sensibles: {}",  sensitiveKeys);
            propertiesManager.setSensitiveKeys(sensitiveKeys);

            // Obtención de las claves sensibles una vez almacenadaas
            log.info("Obtención de las claves sensibles: {}", propertiesManager.getSensitiveKeys());


        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Carga propiedades de prueba en memoria.
     */
    private static void loadTestProperties() {
        log.info("--- Cargando propiedades desde los ficheros ubicados en: {} ---", Constantes.DEFAULT_CONFIG_DIR);

        try {

            propertiesManager.loadAllProperties();
            log.info("Leídas todas las propiedades de todos los ficheros.");

            log.info("Impresión de todas las propiedades definidas en el fichero: {}", Constantes.APP_PROPERTIES);
            propertiesManager.printProperties(Constantes.APP_PROPERTIES);

            log.info("Impresión de todas las propiedades definidas en el fichero: {}", Constantes.EMAIL_PROPERTIES);
            propertiesManager.printProperties(Constantes.EMAIL_PROPERTIES);

        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Prueba la obtención de propiedades individuales y conjuntos completos.
     */
    private static void testPropertyRetrieval() {
        log.info("--- Probando obtención de propiedades ---");

        try {
            // Obtener propiedades individuales
            String appName = propertiesManager.getProperty(Constantes.APP_PROPERTIES, Constantes.KEY_APP_NAME);
            log.info("app.name = {}", appName);

            String nonExistent = propertiesManager.getProperty(Constantes.APP_PROPERTIES, Constantes.KEY_NON_EXISTS);
            log.info("Clave inexistente = {}", nonExistent);

            // Obtener propiedades de un archivo completo
            Properties appProperties = propertiesManager.getProperties(Constantes.APP_PROPERTIES);
            log.info("Propiedades de '{}' obtenidas: {} elementos", Constantes.APP_PROPERTIES, appProperties.size());

            // Obtener todas las propiedades
            Map<String, Properties> allProps = propertiesManager.getAllProperties();
            log.info("Total de archivos cargados: {}", allProps.size());
            allProps.keySet().forEach(fileName ->
                    log.info("  - {}: {} propiedades", fileName, allProps.get(fileName).size())
            );

        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Prueba la impresión de propiedades en logs.
     */
    private static void testPropertyPrinting() {
        log.info("--- Probando impresión de propiedades ---");

        try {
            // Imprimir propiedades de un archivo específico
            log.info("Imprimiendo propiedades de '{}':", Constantes.EMAIL_PROPERTIES);
            propertiesManager.printProperties(Constantes.EMAIL_PROPERTIES);

            log.info("Imprimiendo propiedades de '{}' (con valores sensibles enmascarados)", Constantes.APP_PROPERTIES);
            propertiesManager.printProperties(Constantes.APP_PROPERTIES);

            // Imprimir todas las propiedades
            log.info("Imprimiendo TODAS las propiedades:");
            propertiesManager.printAllProperties();

        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Prueba la exportación a formato JSON.
     */
    private static void testJsonExport() {
        log.info("--- Probando exportación a JSON ---");

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

        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Prueba la validación de claves requeridas.
     */
    private static void testKeyValidation() {
        log.info("--- Probando validación de claves requeridas ---");

        try {
            // Validar claves requeridas que existen
            Set<String> requiredAppKeys = Set.of(Constantes.KEY_APP_NAME);
            boolean validApp = propertiesManager.validateRequiredKeys(Constantes.APP_PROPERTIES, requiredAppKeys);
            log.info("Validación de '{}' con claves requeridas: {}", Constantes.APP_PROPERTIES, validApp);

            // Validar claves requeridas que no existen
            Set<String> requiredMissingKeys = Set.of(Constantes.KEY_NON_EXISTS);
            boolean validMissing = propertiesManager.validateRequiredKeys(Constantes.APP_PROPERTIES, requiredMissingKeys);
            log.info("Validación de '{}' con claves faltantes: {}", Constantes.APP_PROPERTIES, validMissing);

            // Validar archivo inexistente
            try {
                boolean validNonExistent = propertiesManager.validateRequiredKeys(Constantes.APP_NON_EXISTS_PROPERTIES, requiredAppKeys);
                log.info("Validación de archivo inexistente: {}", validNonExistent);
            } catch (PropertiesManagerException e) {
                log.error(e.getMessage());
                FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
            }

        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Prueba la funcionalidad de recarga.
     */
    private static void testReload() {
        log.info("--- Probando recarga de propiedades ---");

        try {
            log.info("Ejecutando recarga...");
            propertiesManager.reload();
            log.info("Recarga completada exitosamente");

            // Verificar que las propiedades siguen disponibles después de la recarga
            Map<String, Properties> reloadedProps = propertiesManager.getAllProperties();
            log.info("Archivos disponibles después de la recarga: {}", reloadedProps.size());

        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Prueba casos de error y manejo de excepciones.
     */
    private void testErrorCases() {
        log.info("--- Probando casos de error ---");

        // Intentar acceder a archivo inexistente
        try {
            propertiesManager.getProperty("archivo_inexistente", "clave");
            log.error("Se esperaba una excepción para archivo inexistente");
        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }

        // Intentar usar nombre de archivo inválido
        try {
            propertiesManager.printProperties("");
            log.error("Se esperaba una excepción para nombre vacío");
        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }

        // Intentar configurar directorio inválido
        try {
            propertiesManager.setConfigDir("/ruta/inexistente/completamente");
        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }

    /**
     * Enmascara una cadena para mostrar información sensible de forma segura.
     * @param input valor de entrada
     * @return String enmascarado
     */
    private static String maskString(String input) {
        if (input == null || input.length() <= 4) {
            return "****";
        }
        return input.substring(0, 2) + "****" + input.substring(input.length() - 2);
    }

    /**
     * Método principal para ejecutar la demo del PropertiesManager.
     *
     * @param args Argumentos de línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        log.info("Iniciando demo del PropertiesManager");

        // NOTA: Aquí necesitarías proporcionar una instancia real de PropertiesManager
        // PropertiesManager manager = new PropertiesManagerImpl(); // Tu implementación

        // Para la demo, asumimos que tienes una implementación disponible
        try {
            // Descomenta la siguiente línea cuando tengas la implementación
            // PropertiesManagerDemo demo = new PropertiesManagerDemo(manager);
            // demo.runAllTests();

            runAllTests();

        } catch (PropertiesManagerException e) {
            log.error(e.getMessage());
            FinalDelPrograma.finalizar(TipoFinalEjecucion.ERROR);
        }
    }
}

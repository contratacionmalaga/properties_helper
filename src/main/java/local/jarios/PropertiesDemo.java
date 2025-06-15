package local.jarios;

import local.jarios.properties.config.PropertiesManager;
import local.jarios.properties.exception.PropertiesLoadException;
import local.jarios.utils.Constantes;
import local.jarios.utils.Mensajes;
import local.jarios.versionfrommanifest.service.VersionFromManifestServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Clase principal que muestra el uso del {@link PropertiesManager} para gestionar
 * archivos de propiedades (.properties) en el proyecto.
 * <p>
 * Funcionalidades demostradas:
 * <ul>
 *   <li>Carga y muestra todas las propiedades cargadas.</li>
 *   <li>Obtiene propiedades concretas con fallback a variables de entorno y sistema.</li>
 *   <li>Exporta propiedades a formato JSON con ocultamiento de valores sensibles.</li>
 *   <li>Valida claves requeridas en archivos de propiedades.</li>
 *   <li>Permite recargar las propiedades.</li>
 *   <li>Manejo de excepciones mediante {@link PropertiesLoadException}.</li>
 * </ul>
 * <p>
 * Uso de logs para trazabilidad detallada de cada paso.
 * </p>
 *
 * @author Juan Antonio
 * @version 1.0
 * @since 2024-06-04
 */
@Slf4j
public class PropertiesDemo {

    /**
     * Constructor privado para evitar instanciación.
     */
    private PropertiesDemo() { /* Evitar instanciación */ }

    /**
     * Método principal que ejecuta la demostración de uso del {@link PropertiesManager}.
     *
     * @param args argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {

        log.info(Mensajes.INICIO);

        try {
            // Obtener la instancia singleton
            PropertiesManager propertiesManager = PropertiesManager.getInstance();
            log.info("Instancia PropertiesManager obtenida correctamente.");

            var versionFromManifestService = new VersionFromManifestServiceImpl();
            log.info("Creado el objeto VersionFromManifestService correctamente.");

            String appName = propertiesManager.getProperty(Constantes.APP_PROPERTIES, "app.name");
            log.info("AppName: {}", appName);

            String appVersion = versionFromManifestService.getVersion(PropertiesDemo.class);
            log.info("AppVersion: {}", appVersion);

            // Cargar todas las propiedades desde el directorio de configuración
            propertiesManager.loadAllProperties(Constantes.CONFIG_DIR);
            log.info("Ficheros .properties cargados desde correctamente");

            // Definir claves sensibles para enmascarar en impresiones y exportaciones
            Set<String> clavesSensibles = Set.of("password", "secret", "token", "apikey");
            propertiesManager.setSensitiveKeys(clavesSensibles);
            log.info("Claves sensibles definidas: {}", clavesSensibles);

            // Mostrar listado de ficheros cargados
            log.info("=== LISTADO DE FICHEROS CARGADOS ===");
            propertiesManager.getAllProperties().keySet().forEach(fichero ->
                    log.info("Fichero cargado: {}", fichero));

            // Mostrar todas las propiedades cargadas (con máscara aplicada)
            log.info("=== PROPIEDADES DE TODOS LOS FICHEROS ===");
            propertiesManager.printAllProperties();

            // Nombre del fichero para pruebas puntuales
            String ficheroTest = "email";
            log.info("Realizando pruebas con fichero: '{}'", ficheroTest);

            // Mostrar propiedades de un fichero específico (enmascarando sensibles)
            log.info("=== PROPIEDADES DE '{}' ===", ficheroTest);
            propertiesManager.printProperties(ficheroTest);

            // Obtener valor de una clave específica con fallback a entorno y sistema
            String claveTest = "mail.password";
            String valor = propertiesManager.getProperty(ficheroTest, claveTest);
            log.info("Valor de la clave '{}' en '{}': {}", claveTest, ficheroTest,
                    valor != null ? "[ENCONTRADO]" : "[NO ENCONTRADO]");

            // Validar existencia de claves requeridas en el fichero
            Set<String> clavesRequeridas = Set.of("mail.user", "mail.password", "mail.smtp.host", "mail.smtp.port");
            boolean valido = propertiesManager.validateRequiredKeys(ficheroTest, clavesRequeridas);
            log.info("Validación de claves requeridas en '{}': {}", ficheroTest, valido ? "OK" : "FALTAN CLAVES");

            // Exportar propiedades a JSON con máscara en valores sensibles
            log.info("=== EXPORTAR PROPIEDADES A JSON (con máscara) ===");
            String jsonEnmascarado = propertiesManager.exportPropertiesToJson(ficheroTest, true);
            log.info("JSON exportado:\n{}", jsonEnmascarado);

            // Exportar todas las propiedades a JSON sin máscara
            log.info("=== EXPORTAR TODAS LAS PROPIEDADES A JSON (sin máscara) ===");
            String jsonCompleto = propertiesManager.exportAllPropertiesToJson(false);
            log.info("JSON completo exportado:\n{}", jsonCompleto);

            // Recargar propiedades desde el directorio de configuración
            log.info("=== RECARGA DE PROPIEDADES ===");
            propertiesManager.reload();
            log.info("Recarga de propiedades completada.");

        } catch (PropertiesLoadException e) {
            log.error("Error durante la gestión de propiedades: {}", e.getMessage(), e);
        } finally {
            log.info(Mensajes.FINAL);
        }
    }
}

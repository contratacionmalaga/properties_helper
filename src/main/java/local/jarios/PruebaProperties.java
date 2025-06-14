package local.jarios;

import local.jarios.properties.config.PropertiesManager;
import local.jarios.properties.exception.PropertiesLoadException;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Clase principal que muestra el uso del {@link PropertiesManager} para gestionar
 * archivos de propiedades (.properties) en el proyecto.
 * <p>
 * Funcionalidades:
 * <ul>
 *   <li>Carga y muestra todas las propiedades cargadas.</li>
 *   <li>Obtiene propiedades concretas.</li>
 *   <li>Exporta propiedades a formato JSON con ocultamiento de valores sensibles.</li>
 *   <li>Valida claves requeridas en archivos de propiedades.</li>
 *   <li>Permite recargar las propiedades.</li>
 * </ul>
 * <p>
 * Muestra manejo de excepciones mediante {@link PropertiesLoadException}.
 *
 * @author Juan Antonio
 * @version 1.0
 * @since 2024-06-04
 */
@Slf4j
public class PruebaProperties {

    /**
     * Constructor privado.
     */
    private PruebaProperties() { /*   */}

    /**
     * Punto de entrada de la aplicación.
     * Ejecuta ejemplos de uso completo de {@link PropertiesManager}.
     *
     * @param args argumentos de línea de comando (no usados)
     */
    public static void main(String[] args) {
        try {
            // Obtener instancia Singleton
            PropertiesManager manager = PropertiesManager.getInstance();

            // === Configuración inicial ===
            Set<String> clavesSensibles = Set.of("password", "secret", "token", "apikey");
            manager.setSensitiveKeys(clavesSensibles);  // Ahora se aplica sobre la instancia

            // Listado de todos los ficheros cargados
            log.info("=== LISTA DE FICHEROS CARGADOS ===");
            manager.getAllProperties().keySet().forEach(file ->
                    log.info("Fichero cargado: {}", file));

            // Obtener y mostrar las propiedades de un fichero específico
            log.info("=== PROPIEDADES DE TODOS LOS FICHEROS ===");
            manager.printAllProperties();

            String testFile = "email";

            // Imprimir propiedades con enmascaramiento de sensibles
            log.info("=== IMPRIMIR UN CON MÁSCARA ===");
            manager.printProperties(testFile);

            // Imprimir todas las propiedades de todos los ficheros
            log.info("=== TODAS LAS PROPIEDADES ===");
            manager.printAllProperties();

            // Obtener una clave individual (fallback: env, system)
            log.info("=== getProperty con fallback ===");
            String key = "mail.password";
            String valor = manager.getProperty(testFile, key);
            log.info("Valor de {}}: {}", key, valor != null ? valor : "[no encontrada]");

            // Validar claves requeridas
            log.info("=== VALIDACIÓN DE CLAVES REQUERIDAS ===");
            manager.validateRequiredKeys(testFile, Set.of("mail.user", "mail.password", "mail.smtp.host", "mail.smtp.port"));

            // Exportar a JSON
            log.info("=== EXPORT A JSON ===");
            String json = manager.exportAsJson(testFile, true);
            log.info("JSON exportado (con máscara):\n{}", json);

            // Exportar todos a JSON
            log.info("=== EXPORT TODO A JSON ===");
            String fullJson = manager.exportAllAsJson(false);
            log.info("Todos los ficheros exportados:\n{}", fullJson);

            // Imprimir en log con formato JSON
            log.info("=== PRINT JSON A LOG (con máscara) ===");
            manager.printAllAsJson(true);

            // Recargar propiedades
            log.info("=== RECARGA DE PROPIEDADES ===");
            manager.reload();
            log.info("Recarga completada.");

        } catch (PropertiesLoadException ex) {
            log.error("[ERROR] {}", ex.getMessage(), ex);
        }
    }
}

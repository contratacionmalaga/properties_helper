package local.jarios.properties;

import local.jarios.properties.api.PropertiesManagerService;
import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.api.PropertiesManagerServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class PropertiesManagerDemo {


    public static void main(String[] args) {
        log.info("=== INICIO DEMO PropertiesManagerService ===");

        // Obtiene la instancia singleton del gestor de propiedades
        PropertiesManagerService manager = PropertiesManagerServiceImpl.getInstance();

        // Test 1: Configurar directorio y clave secreta
        log.info("[TEST 1] Configurar directorio y clave secreta");
        manager.setConfigDir("config"); // Ajusta según tu ruta
        manager.setSecretKey("miClaveSecretaDemo");
        log.info("Directorio configuración: {}", manager.getConfigDir());
        log.info("Clave secreta activa: {}", manager.getSecretKey());

        // Test 2: Definir claves sensibles
        log.info("[TEST 2] Establecer claves sensibles");
        Set<String> clavesSensibles = new HashSet<>(Arrays.asList("password", "secret.key", "token"));
        manager.setSensitiveKeys(clavesSensibles);
        log.info("Claves sensibles definidas: {}", clavesSensibles);

        // Test 3: Cargar todas las propiedades
        log.info("[TEST 3] Cargar todas las propiedades");
        manager.loadAllProperties();

        // Test 4: Imprimir todas las propiedades (con enmascarado)
        log.info("[TEST 4] Imprimir todas las propiedades cargadas (claves sensibles ocultas)");
        manager.printAllProperties();

        // Test 5: Exportar propiedades a JSON (con y sin enmascarado)
        log.info("[TEST 5] Exportar propiedades a JSON");
        try {
            String jsonMasked = manager.exportAllPropertiesToJson(true);
            log.info("JSON con valores sensibles enmascarados:\n{}", jsonMasked);

            String jsonClear = manager.exportAllPropertiesToJson(false);
            log.info("JSON con valores sensibles sin enmascarar:\n{}", jsonClear);
        } catch (PropertiesManagerException e) {
            log.error("Error exportando propiedades a JSON", e);
        }

        // Test 6: Imprimir propiedades de fichero específico
        String fichero = "app";
        log.info("[TEST 6] Imprimir propiedades del fichero '{}'", fichero);
        try {
            manager.printProperties(fichero);
        } catch (PropertiesManagerException e) {
            log.error("No se encontró fichero de propiedades '{}'", fichero, e);
        }

        // Test 7: Obtener propiedad individual con búsqueda jerárquica
        String claveBuscada = "app.name";
        log.info("[TEST 7] Obtener propiedad individual '{}' del fichero '{}'", claveBuscada, fichero);
        String valor = manager.getProperty(fichero, claveBuscada);
        if (valor != null) {
            log.info("Valor obtenido: {}", valor);
        } else {
            log.warn("Clave '{}' no encontrada en fichero '{}', ni en variables de entorno ni propiedades del sistema", claveBuscada, fichero);
        }

        // Test 8: Validar claves requeridas en un fichero
        log.info("[TEST 8] Validar claves requeridas en el fichero '{}'", fichero);
        Set<String> clavesRequeridas = new HashSet<>(Arrays.asList("app.name", "app.version", "app.author"));
        boolean valid = manager.validateRequiredKeys(fichero, clavesRequeridas);
        if (valid) {
            log.info("El fichero '{}' contiene todas las claves requeridas: {}", fichero, clavesRequeridas);
        } else {
            log.warn("El fichero '{}' NO contiene todas las claves requeridas: {}", fichero, clavesRequeridas);
        }

        // Test 9: Recargar todas las propiedades
        log.info("[TEST 9] Recargar propiedades");
        manager.reload();
        log.info("Recarga completada");

        log.info("=== FIN DEMO PropertiesManagerService ===");
    }
}

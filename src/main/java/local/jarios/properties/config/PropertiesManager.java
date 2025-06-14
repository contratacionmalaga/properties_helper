package local.jarios.properties.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.jarios.properties.exception.PropertiesLoadException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestión y carga centralizada de ficheros .properties desde carpeta externa o classpath.
 * <p>
 * Implementa patrón Singleton thread-safe para acceso global.
 * <p>
 * Funcionalidades principales:
 * <ul>
 *   <li>Carga de múltiples ficheros .properties con fallback a recursos JAR.</li>
 *   <li>Inmutabilidad de las propiedades para evitar modificaciones accidentales.</li>
 *   <li>Ocultamiento de claves sensibles en impresión y exportación.</li>
 *   <li>Exportación a JSON de un fichero o todos.</li>
 *   <li>Validación de claves requeridas en un fichero.</li>
 *   <li>Recarga segura y sincronizada de propiedades.</li>
 * </ul>
 *
 * @author Juan Antonio
 * @version 1.0
 * @since 2024-06-04
 */
@Slf4j
public class PropertiesManager {

    private static final String PROPERTIES_EXT = ".properties";
    private static final String CONFIG_DIR = "config";

    private static volatile PropertiesManager instance;

    // Mapa inmutable tras carga
    private Map<String, Properties> propertiesMap;

    private static final ObjectMapper mapper = new ObjectMapper();

    // Claves sensibles para ocultar
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "api.key", "token", "accessKey", "privateKey"
    );

    /**
     * Constructor privado para patrón Singleton.
     * Carga todas las properties al inicializar la instancia.
     */
    private PropertiesManager() {
        loadAllProperties();
    }

    /**
     * Método estático para obtener la instancia única de la clase (Singleton).
     * Implementa doble verificación para asegurar thread safety.
     *
     * @return instancia única de PropertiesManager
     */
    public static PropertiesManager getInstance() {
        if (instance == null) {
            synchronized (PropertiesManager.class) {
                if (instance == null) {
                    instance = new PropertiesManager();
                }
            }
        }
        return instance;
    }

    /**
     * Carga todas las properties desde carpeta externa o recursos del classpath.
     * Construye un mapa inmutable y properties inmutables para proteger la configuración.
     * Si no existe la carpeta externa 'config', intenta cargar desde recursos empaquetados.
     *
     * @throws PropertiesLoadException si ocurre un error al leer cualquier fichero.
     */
    private void loadAllProperties() {
        Map<String, Properties> tempMap = new HashMap<>();

        File configDir = new File(CONFIG_DIR);
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(PROPERTIES_EXT));
            if (files != null) {
                for (File file : files) {
                    Properties props = loadPropertiesFromFile(file);
                    tempMap.put(stripExtension(file.getName()), makeImmutable(props));
                }
            }
        } else {
            log.warn("Directorio '{}' no encontrado. Intentando cargar desde recursos del JAR...", CONFIG_DIR);
            // Lista fija de recursos para cargar desde JAR (puedes modificar o parametrizar)
            List<String> resourcesToLoad = List.of("app.properties", "db.properties");
            for (String resourceName : resourcesToLoad) {

                Optional.of(loadPropertiesFromResource(String.format("%s/%s", CONFIG_DIR, resourceName)))
                        .ifPresent(props -> tempMap.put(stripExtension(resourceName), makeImmutable(props)));
            }
        }

        // Mapa inmutable para evitar modificaciones accidentales
        this.propertiesMap = Collections.unmodifiableMap(tempMap);
        log.info("Cargados {} ficheros .properties", propertiesMap.size());
    }

    /**
     * Carga un fichero .properties desde un archivo externo en disco.
     *
     * @param file archivo .properties
     * @return Properties cargadas
     * @throws PropertiesLoadException si falla la lectura
     */
    private Properties loadPropertiesFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            log.debug("Cargado archivo externo: {}", file.getAbsolutePath());
            return props;
        } catch (IOException e) {
            String msg = String.format("Error leyendo el archivo: %s", file.getName());
            log.error(msg, e);
            throw new PropertiesLoadException(msg, e);
        }
    }

    /**
     * Carga un fichero .properties desde los recursos empaquetados en el JAR (classpath).
     *
     * @param resourcePath ruta del recurso en classpath
     * @return Properties cargadas o null si no existe recurso
     * @throws PropertiesLoadException si falla la lectura
     */
    private Properties loadPropertiesFromResource(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Recurso '{}' no encontrado en classpath.", resourcePath);
                return new Properties();
            }
            Properties props = new Properties();
            props.load(is);
            log.debug("Cargado recurso JAR: {}", resourcePath);
            return props;
        } catch (IOException e) {
            String msg = String.format("Error leyendo recurso: %s", resourcePath);
            log.error(msg, e);
            throw new PropertiesLoadException(msg, e);
        }
    }

    /**
     * Crea una versión inmutable de un objeto Properties para evitar modificaciones.
     *
     * @param original Properties original mutable
     * @return Properties inmutable que lanza UnsupportedOperationException si se intenta modificar
     */
    private Properties makeImmutable(Properties original) {
        Properties copy = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                throw new UnsupportedOperationException("Propiedades inmutables");
            }

            @Override
            public synchronized Object remove(Object key) {
                throw new UnsupportedOperationException("Propiedades inmutables");
            }

            @Override
            public synchronized void clear() {
                throw new UnsupportedOperationException("Propiedades inmutables");
            }
        };
        copy.putAll(original);
        return copy;
    }

    /**
     * Quita la extensión '.properties' de un nombre de fichero.
     *
     * @param filename nombre de fichero
     * @return nombre sin extensión
     */
    private String stripExtension(String filename) {
        if (filename == null) return null;
        if (filename.endsWith(PROPERTIES_EXT)) {
            return filename.substring(0, filename.length() - PROPERTIES_EXT.length());
        }
        return filename;
    }

    /**
     * Imprime todas las propiedades de un fichero específico en el log,
     * ocultando valores sensibles.
     *
     * @param fileNameWithoutExtension nombre del fichero sin extensión
     * @throws PropertiesLoadException si no existe el fichero
     */
    public void printProperties(String fileNameWithoutExtension) {
        Properties props = propertiesMap.get(fileNameWithoutExtension);
        if (props == null) {
            throw new PropertiesLoadException("No se encontró el fichero: " + fileNameWithoutExtension + PROPERTIES_EXT);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("---- [").append(fileNameWithoutExtension).append(PROPERTIES_EXT).append("] ----\n");
        props.forEach((key, value) -> {
            String val = isSensitiveKey(key.toString()) ? "*****" : value.toString();
            sb.append(key).append(" = ").append(val).append("\n");
        });
        sb.append("----------------------------------------");
        log.info(sb.toString());
    }

    /**
     * Imprime todas las propiedades de todos los ficheros cargados.
     * Si no hay ficheros cargados, informa en log.
     */
    public void printAllProperties() {
        if (propertiesMap.isEmpty()) {
            log.info("No se han cargado ficheros .properties.");
            return;
        }
        propertiesMap.forEach((fileNameWithoutExtension, props) ->
                printProperties(fileNameWithoutExtension)
        );
    }

    /**
     * Devuelve una copia inmutable de las propiedades de un fichero específico.
     * Retorna null si no existe el fichero.
     *
     * @param fileNameWithoutExtension nombre fichero sin extensión
     * @return Properties inmutable o null si no existe
     */
    public Properties getProperties(String fileNameWithoutExtension) {
        Properties props = propertiesMap.get(fileNameWithoutExtension);
        if (props == null) return new Properties();

        return makeImmutable(props);
    }

    /**
     * Obtiene el valor de una clave buscando primero en el fichero properties,
     * si no existe busca en variables de entorno y luego en propiedades del sistema.
     *
     * @param fileName nombre fichero properties sin extensión
     * @param key clave a buscar
     * @return valor encontrado o null si no existe en ningún lugar
     */
    public String getProperty(String fileName, String key) {
        Properties props = propertiesMap.get(fileName);
        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        }
        // fallback variables entorno y sistema
        String env = System.getenv(key);
        if (env != null) return env;
        return System.getProperty(key);
    }

    /**
     * Devuelve el mapa completo de propiedades cargadas.
     * Tanto el mapa como las Properties son inmutables.
     *
     * @return mapa inmutable de propiedades
     */
    public Map<String, Properties> getAllProperties() {
        return propertiesMap;
    }

    /**
     * Determina si una clave es considerada sensible (para ocultar su valor).
     * La comparación es case-insensitive y busca si contiene alguna palabra sensible.
     *
     * @param key clave a evaluar
     * @return true si es sensible, false en caso contrario
     */
    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.stream()
                .anyMatch(sensitive -> key.toLowerCase(Locale.ROOT).contains(sensitive.toLowerCase(Locale.ROOT)));
    }

    /**
     * Exporta un fichero properties a formato JSON, opcionalmente ocultando
     * valores sensibles.
     *
     * @param fileName nombre fichero sin extensión
     * @param maskSensitiveValues true para ocultar valores sensibles
     * @return JSON formateado con las propiedades
     * @throws PropertiesLoadException si el fichero no existe o error JSON
     */
    public String exportAsJson(String fileName, boolean maskSensitiveValues) {
        Properties props = propertiesMap.get(fileName);
        if (props == null) {
            throw new PropertiesLoadException("No se encontró el fichero: " + fileName + PROPERTIES_EXT);
        }
        Map<String, String> safeMap = props.stringPropertyNames()
                .stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> maskSensitiveValues && isSensitiveKey(key) ? "*****" : props.getProperty(key)
                ));
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(safeMap);
        } catch (JsonProcessingException e) {
            String msg = "Error exportando a JSON";
            log.error(msg, e);
            throw new PropertiesLoadException(msg, e);
        }
    }

    /**
     * Exporta todas las properties cargadas a JSON, opcionalmente ocultando
     * valores sensibles.
     *
     * @param maskSensitiveValues true para ocultar valores sensibles
     * @return JSON formateado con todas las propiedades agrupadas por fichero
     * @throws PropertiesLoadException si error en exportación JSON
     */
    public String exportAllAsJson(boolean maskSensitiveValues) {
        Map<String, Map<String, String>> allSafeProps = new HashMap<>();
        propertiesMap.forEach((fileName, props) -> {
            Map<String, String> safeProps = props.stringPropertyNames()
                    .stream()
                    .collect(Collectors.toMap(
                            key -> key,
                            key -> maskSensitiveValues && isSensitiveKey(key) ? "*****" : props.getProperty(key)
                    ));
            allSafeProps.put(fileName, safeProps);
        });
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allSafeProps);
        } catch (JsonProcessingException e) {
            String msg = "Error exportando todo a JSON";
            log.error(msg, e);
            throw new PropertiesLoadException(msg, e);
        }
    }

    /**
     * Valida que un fichero properties contenga un conjunto de claves requeridas.
     * Lanza excepción si alguna clave falta.
     *
     * @param fileName nombre fichero sin extensión
     * @param requiredKeys conjunto de claves obligatorias
     * @throws PropertiesLoadException si falta alguna clave o fichero no existe
     */
    public void validateRequiredKeys(String fileName, Set<String> requiredKeys) {
        Properties props = propertiesMap.get(fileName);
        if (props == null) {
            String msg = String.format("No se encontró el fichero: %s%s", fileName, PROPERTIES_EXT);
            log.error(msg);
            throw new PropertiesLoadException(msg);
        }
        for (String key : requiredKeys) {
            if (!props.containsKey(key)) {
                String msg = String.format("Clave requerida faltante: %s en %s%s", key, fileName, PROPERTIES_EXT);
                log.error(msg);
                throw new PropertiesLoadException(msg);
            }
        }
    }

    /**
     * Recarga todas las propiedades desde la carpeta o recursos,
     * reemplazando el mapa actual con uno nuevo e inmutable.
     * Método sincronizado para evitar problemas concurrentes.
     */
    public synchronized void reload() {
        loadAllProperties();
        log.info("Recargadas todas las propiedades");
    }
}

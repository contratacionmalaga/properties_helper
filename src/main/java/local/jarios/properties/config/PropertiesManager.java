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

    /**
     * Instancia única para el patrón Singleton.
     */
    private static final PropertiesManager INSTANCE = new PropertiesManager();

    /**
     * Valor utilizado para ocultar los valores asociados a las key-sensitives.
     */
    private static final String KEY_SENSITIVE_VALUE = "******";

    /**
     * Extensión estándar de ficheros properties.
     */
    private static final String PROPERTIES_EXT = ".properties";

    /**
     * Directorio de configuración esperado para ficheros properties.
     */
    private static final String CONFIG_DIR = "config";

    /**
     * Mapa inmutable que contiene el conjunto de propiedades cargadas,
     * donde la clave es el nombre del fichero sin extensión.
     */
    private Map<String, Properties> propertiesMap;

    /**
     * Objeto Jackson para serialización JSON.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Conjunto de claves consideradas sensibles y que deben ser ocultadas.
     * Ahora es un campo de instancia para poder configurarlo desde fuera.
     */
    private Set<String> sensitiveKeys = Collections.emptySet();

    /**
     * Constructor privado para patrón Singleton.
     * Realiza la carga inicial de todos los ficheros properties.
     */
    private PropertiesManager() {
        loadAllProperties();
    }

    /**
     * Obtiene la instancia única de {@code PropertiesManager}.
     *
     * @return instancia singleton
     */
    public static PropertiesManager getInstance() {
        return INSTANCE;
    }

    /**
     * Permite establecer las claves sensibles que deben ser ocultadas.
     * Se puede llamar tras obtener la instancia.
     *
     * @param keys conjunto de claves sensibles
     */
    public void setSensitiveKeys(Set<String> keys) {
        if (keys == null) {
            this.sensitiveKeys = Collections.emptySet();
        } else {
            this.sensitiveKeys = Set.copyOf(keys);
        }
        log.debug("[setSensitiveKeys] Claves sensibles configuradas: {}", this.sensitiveKeys);
    }

    /**
     * Carga todos los ficheros properties disponibles desde el directorio externo
     * o desde los recursos empaquetados en el classpath.
     * Construye un mapa inmutable de propiedades.
     *
     * @throws PropertiesLoadException si ocurre un error durante la lectura
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
            // Lista fija de recursos para cargar desde JAR (se puede parametrizar)
            List<String> resourcesToLoad = List.of("app.properties", "db.properties");
            for (String resourceName : resourcesToLoad) {
                Optional.of(loadPropertiesFromResource(String.format("%s/%s", CONFIG_DIR, resourceName)))
                        .ifPresent(props -> tempMap.put(stripExtension(resourceName), makeImmutable(props)));
            }
        }

        this.propertiesMap = Collections.unmodifiableMap(tempMap);
        log.debug("Cargados {} ficheros .properties", propertiesMap.size());

    }

    /**
     * Carga un fichero properties desde un archivo externo.
     *
     * @param file archivo .properties a cargar
     * @return objeto Properties cargado
     * @throws PropertiesLoadException si ocurre error durante la lectura
     */
    private Properties loadPropertiesFromFile(File file) {
        log.debug("[loadPropertiesFromFile] Inicio carga fichero externo: {}", file.getAbsolutePath());
        long start = System.nanoTime();
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            long duration = System.nanoTime() - start;
            log.debug("[loadPropertiesFromFile] Cargado fichero '{}' con {} claves en {} ms",
                    file.getName(), props.size(), duration / 1_000_000);
            return props;
        } catch (IOException e) {
            String msg = String.format("Error leyendo el archivo: %s", file.getName());
            log.error("[loadPropertiesFromFile] {}", msg, e);
            throw new PropertiesLoadException(msg, e);
        }
    }

    /**
     * Carga un fichero properties desde un recurso empaquetado en el classpath.
     *
     * @param resourcePath ruta del recurso (e.g. "config/app.properties")
     * @return objeto Properties cargado o propiedades vacías si no existe recurso
     * @throws PropertiesLoadException si ocurre error de lectura
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
     * Crea una copia inmutable del objeto Properties recibido,
     * para evitar modificaciones posteriores.
     *
     * @param original Properties original mutable
     * @return Properties inmutable que lanza excepción si se intenta modificar
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
     * Quita la extensión {@code .properties} del nombre del fichero.
     *
     * @param filename nombre del fichero completo
     * @return nombre sin la extensión
     */
    private String stripExtension(String filename) {
        if (filename == null) return null;
        if (filename.endsWith(PROPERTIES_EXT)) {
            return filename.substring(0, filename.length() - PROPERTIES_EXT.length());
        }
        return filename;
    }

    /**
     * Imprime en el log todas las propiedades de un fichero dado,
     * ocultando los valores sensibles.
     *
     * @param fileNameWithoutExtension nombre del fichero sin extensión
     * @throws PropertiesLoadException si el fichero no existe
     */
    public void printProperties(String fileNameWithoutExtension) {
        Properties props = propertiesMap.get(fileNameWithoutExtension);
        if (props == null) {
            String msg = String.format("No se encontró el fichero: %s", fileNameWithoutExtension + PROPERTIES_EXT);
            throw new PropertiesLoadException(msg);
        }

        log.info(">>> {}", fileNameWithoutExtension + PROPERTIES_EXT);
        props.forEach((key, value) -> {
            String val = isSensitiveKey(key.toString()) ? KEY_SENSITIVE_VALUE : value.toString();
            log.info("{} = {}", key, val);
        });
    }

    /**
     * Imprime en el log todas las propiedades de todos los ficheros cargados.
     * Si no hay ficheros, lo indica en log.
     */
    public void printAllProperties() {
        if (propertiesMap.isEmpty()) {
            log.info("No se han cargado ficheros .properties.");
            return;
        }
        propertiesMap.forEach((fileNameWithoutExtension, props) -> printProperties(fileNameWithoutExtension));
    }

    /**
     * Devuelve una copia inmutable de las propiedades de un fichero.
     *
     * @param fileNameWithoutExtension nombre fichero sin extensión
     * @return Properties inmutable (vacías si no existe fichero)
     */
    public Properties getProperties(String fileNameWithoutExtension) {
        Properties props = propertiesMap.get(fileNameWithoutExtension);
        if (props == null) return new Properties();

        return makeImmutable(props);
    }

    /**
     * Obtiene el valor de una clave dada buscando en orden:
     * primero en el fichero properties especificado, luego en variables de entorno,
     * y finalmente en propiedades del sistema.
     *
     * @param fileName nombre del fichero sin extensión
     * @param key clave a buscar
     * @return valor encontrado o {@code null} si no existe
     */
    public String getProperty(String fileName, String key) {
        Properties props = propertiesMap.get(fileName);
        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        }
        String env = System.getenv(key);
        if (env != null) return env;
        return System.getProperty(key);
    }

    /**
     * Devuelve el mapa completo e inmutable de propiedades cargadas.
     *
     * @return mapa con claves fichero y valores Properties inmutables
     */
    public Map<String, Properties> getAllProperties() {
        return propertiesMap;
    }

    /**
     * Determina si una clave debe considerarse sensible para ocultar su valor.
     * La comparación no distingue mayúsculas y minúsculas.
     *
     * @param key clave a evaluar
     * @return {@code true} si la clave es sensible; {@code false} en otro caso
     */
    private boolean isSensitiveKey(String key) {
        String keyLower = key.toLowerCase(Locale.ROOT);
        for (String sensitive : sensitiveKeys) {
            log.debug("[isSensitiveKey] Comprobando '{}' contra '{}'", keyLower, sensitive.toLowerCase(Locale.ROOT));
            if (keyLower.contains(sensitive.toLowerCase(Locale.ROOT))) {
                log.debug("[isSensitiveKey] La clave '{}' es considerada sensible.", key);
                return true;
            }
        }
        return false;
    }

    /**
     * Exporta un fichero properties a formato JSON.
     * Opcionalmente oculta valores sensibles.
     *
     * @param fileName nombre fichero sin extensión
     * @param maskSensitiveValues {@code true} para ocultar valores sensibles
     * @return cadena JSON con las propiedades
     * @throws PropertiesLoadException si el fichero no existe o falla la conversión JSON
     */
    public String exportAsJson(String fileName, boolean maskSensitiveValues) {
        Properties props = propertiesMap.get(fileName);
        if (props == null) {
            String msg = String.format("No se encontró el fichero: %s", fileName + PROPERTIES_EXT);
            throw new PropertiesLoadException(msg);
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
     * Exporta todas las properties cargadas a formato JSON agrupadas por fichero.
     * Opcionalmente oculta valores sensibles.
     *
     * @param maskSensitiveValues {@code true} para ocultar valores sensibles
     * @return cadena JSON con todas las propiedades agrupadas
     * @throws PropertiesLoadException si falla la conversión JSON
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
     * Valida que un fichero properties contenga todas las claves indicadas.
     * Si falta alguna clave, lanza excepción.
     *
     * @param fileName nombre fichero sin extensión
     * @param requiredKeys conjunto de claves requeridas
     * @throws PropertiesLoadException si falta alguna clave o fichero no existe
     */
    public void validateRequiredKeys(String fileName, Set<String> requiredKeys) {
        log.debug("[validateRequiredKeys] Validando claves requeridas en fichero: {}", fileName);
        Properties props = propertiesMap.get(fileName);
        if (props == null) {
            String msg = String.format("No se encontró el fichero: %s%s", fileName, PROPERTIES_EXT);
            log.error("[validateRequiredKeys] {}", msg);
            throw new PropertiesLoadException(msg);
        }
        for (String key : requiredKeys) {
            if (!props.containsKey(key)) {
                String msg = String.format("Clave requerida faltante: %s en %s%s", key, fileName, PROPERTIES_EXT);
                log.warn("[validateRequiredKeys] {}", msg);
                throw new PropertiesLoadException(msg);
            }
        }
        log.info("[validateRequiredKeys] Todas las claves requeridas están presentes en '{}'", fileName);
    }

    /**
     * Recarga todas las propiedades desde la fuente original.
     * Reemplaza el mapa actual por uno nuevo y sincroniza el método para evitar
     * condiciones de carrera en entornos concurrentes.
     */
    public synchronized void reload() {
        log.info("[reload] Inicio recarga de propiedades");
        long start = System.nanoTime();
        loadAllProperties();
        long duration = System.nanoTime() - start;
        log.info("[reload] Recarga completada en {} ms", duration / 1_000_000);
    }

    /**
     * Imprime en el log todas las propiedades cargadas en formato JSON,
     * agrupadas por fichero. Los valores sensibles se pueden enmascarar.
     *
     * @param maskSensitiveValues {@code true} para ocultar claves sensibles; {@code false} para mostrarlas tal cual
     */
    public void printAllAsJson(boolean maskSensitiveValues) {
        log.info("[printAllAsJson] Exportando propiedades en formato JSON (ocultar sensibles: {})", maskSensitiveValues);
        try {
            String json = exportAllAsJson(maskSensitiveValues);
            log.info("----[PROPERTIES AS JSON]----\n{}\n-----------------------------", json);
        } catch (PropertiesLoadException e) {
            log.error("[printAllAsJson] Error al exportar propiedades como JSON", e);
        }
    }
}

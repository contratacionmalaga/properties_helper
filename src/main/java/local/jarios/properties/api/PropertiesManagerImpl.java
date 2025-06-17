package local.jarios.properties.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.utils.Constantes;
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
 * @version 1.1
 * @since 2024-06-04
 */
@Slf4j
public class PropertiesManagerImpl {

    /**
     * Ruta por defecto si no se establece otra.
     */
    private static final String DEFAULT_CONFIG_DIR = Constantes.CONFIG_DIR;

    /**
     * Ruta actual desde la que se cargan los .properties
     */
    private String configDir = DEFAULT_CONFIG_DIR;

    /**
     * Clave por defecto si no se establece otra.
     */
    private static final String DEFAULT_SECRET_KEY = "defaultKey123"; // solo para desarrollo

    /**
     * Clave actual usada para desencriptar valores sensibles.
     */
    private String secretKey = DEFAULT_SECRET_KEY;

    /**
     * Instancia única (singleton) del gestor de propiedades.
     * Se inicializa de forma temprana y segura al cargar la clase.
     */
    private static final PropertiesManagerImpl INSTANCE = new PropertiesManagerImpl();

    /**
     * Valor que se usará para enmascarar claves sensibles durante la impresión o exportación.
     * Útil para ocultar contraseñas, tokens, etc.
     */
    private static final String KEY_SENSITIVE_VALUE = "******";

    /**
     * Extensión estándar usada para identificar ficheros de propiedades (.properties).
     * Se utiliza para filtrar archivos o construir rutas.
     */
    private static final String PROPERTIES_EXT = ".properties";

    /**
     * Mapa inmutable con el conjunto de propiedades cargadas,
     * donde la clave es el nombre del fichero sin extensión.
     */
    private Map<String, Properties> propertiesMap = Collections.emptyMap();

    /**
     * Instancia de {@link ObjectMapper} utilizada para convertir propiedades a formato JSON.
     * Configurada con el generador por defecto de Jackson.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Conjunto de claves consideradas sensibles y que deben ser ocultadas.
     */
    private Set<String> sensitiveKeys = Collections.emptySet();

    /**
     * Constructor vacío
     */
    private PropertiesManagerImpl() {
        // Constructor privado Singleton
    }

    /**
     * Obtiene la instancia única de {@code PropertiesManager}.
     *
     * @return instancia singleton
     */
    public static PropertiesManagerImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Establece las claves sensibles que deben ser ocultadas.
     *
     * @param keys conjunto de claves sensibles (puede ser {@code null})
     */
    public void setSensitiveKeys(
            Set<String> keys
    ) {
        this.sensitiveKeys = (keys == null) ? Collections.emptySet() : Set.copyOf(keys);
        log.debug("[setSensitiveKeys] Claves sensibles configuradas: {}", this.sensitiveKeys);
    }

    /**
     * Carga todos los ficheros .properties desde un directorio,
     * o desde recursos JAR si no se encuentra el directorio.
     * Reemplaza completamente el mapa interno de propiedades.
     *
     * @param dirPath ruta al directorio con ficheros .properties
     */
    public synchronized void loadAllProperties(
            String dirPath
    ) {
        log.debug("[loadAllProperties] - Cargando todas las propiedades desde: {}", dirPath);
        Map<String, Properties> tempMap = new HashMap<>();

        File configDir = new File(dirPath);
        log.debug("[loadAllProperties] - Path absoluto: {}", configDir.getAbsolutePath());
        if (configDir.exists() && configDir.isDirectory()) {
            log.debug("[loadAllProperties] - El directorio es correcto.");
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(PROPERTIES_EXT));
            if (files != null) {
                log.debug("[loadAllProperties] - Número de ficheros en el directorio: {}", files.length);
                for (File file : files) {
                    log.debug("[loadAllProperties] - Procesando fichero: {}", file.getName());
                    try {
                        Properties props = loadPropertiesFromFile(file);
                        if (log.isDebugEnabled()) {
                            printProperties(props);
                        }
                        tempMap.put(stripExtension(file.getName()), makeImmutable(props));
                    } catch (PropertiesManagerException e) {
                        log.warn("[loadAllProperties] No se pudo cargar '{}': {}", file.getName(), e.getMessage());
                    }
                }
            }
        } else {
            log.warn("Directorio '{}' no encontrado. Intentando cargar desde recursos del JAR.", dirPath);
            List<String> resourcesToLoad = List.of("app.properties", "db.properties");
            for (String resourceName : resourcesToLoad) {
                try {
                    Properties props = loadPropertiesFromResource(dirPath + "/" + resourceName);
                    tempMap.put(stripExtension(resourceName), makeImmutable(props));
                } catch (PropertiesManagerException e) {
                    log.warn("[loadAllProperties] No se pudo cargar recurso '{}': {}", resourceName, e.getMessage());
                }
            }
        }

        propertiesMap = Collections.unmodifiableMap(tempMap);
        log.debug("Cargados {} ficheros .properties desde '{}'", propertiesMap.size(), dirPath);
    }

    /**
     * Carga propiedades desde un fichero dado.
     *
     * @param file archivo .properties
     * @return Properties cargadas
     * @throws PropertiesManagerException en caso de error
     */
    private Properties loadPropertiesFromFile(
            File file
    ) {
        log.debug("[loadPropertiesFromFile] - Cargando todas las propiedades desde: {}", file.getName());
        try (InputStream is = new FileInputStream(file)) {
            log.debug("[loadPropertiesFromFile] - Fichero cargado correctamente.");
            Properties props = new Properties();
            props.load(is);
            if (log.isDebugEnabled()) {
                printProperties(props);
            }
            return props;
        } catch (IOException e) {
            throw new PropertiesManagerException("Error cargando fichero: " + file.getName(), e);
        }
    }

    /**
     * Carga propiedades desde recurso del classpath.
     *
     * @param resourcePath ruta del recurso dentro del classpath
     * @return Properties cargadas
     * @throws PropertiesManagerException en caso de error o recurso no encontrado
     */
    private Properties loadPropertiesFromResource(
            String resourcePath
    ) {
        log.debug("[loadPropertiesFromResource] - Cargando todas las propiedades desde resource: {}", resourcePath);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            log.debug("[loadPropertiesFromResource] - Fichero cargado correctamente.");
            if (is == null) {
                throw new PropertiesManagerException("Recurso no encontrado: " + resourcePath);
            }
            Properties props = new Properties();
            props.load(is);
            if (log.isDebugEnabled()) {
                printProperties(props);
            }
            return props;
        } catch (IOException e) {
            throw new PropertiesManagerException("Error cargando recurso: " + resourcePath, e);
        }
    }

    /**
     * Crea una copia inmutable del objeto Properties recibido,
     * para evitar modificaciones posteriores.
     *
     * @param original Properties original mutable
     * @return Properties inmutable que lanza excepción si se intenta modificar
     */
    private Properties makeImmutable(
            Properties original
    ) {
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
     * Imprime en el log todas las propiedades de un fichero dado,
     * ocultando los valores sensibles.
     *
     * @param fileNameWithoutExtension nombre del fichero sin extensión
     * @throws PropertiesManagerException si el fichero no existe
     */
    public void printProperties(
            String fileNameWithoutExtension
    ) {
        Properties props = propertiesMap.get(fileNameWithoutExtension);
        if (props == null) {
            String msg = String.format("No se encontró el fichero: %s", fileNameWithoutExtension + PROPERTIES_EXT);
            throw new PropertiesManagerException(msg);
        }
        log.info(">>> {}", fileNameWithoutExtension + PROPERTIES_EXT);
        printProperties(props);
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
        propertiesMap.forEach((
                fileNameWithoutExtension, props) -> printProperties(fileNameWithoutExtension));
    }

    /**
     * Devuelve una copia inmutable de las propiedades de un fichero.
     *
     * @param fileNameWithoutExtension nombre fichero sin extensión
     * @return Properties inmutable (vacías si no existe fichero)
     */
    public Properties getProperties(
            String fileNameWithoutExtension
    ) {
        log.debug("[getProperties] - Propiedades del fichero: {}", fileNameWithoutExtension);
        Properties props = propertiesMap.get(fileNameWithoutExtension);
        if (log.isDebugEnabled()) {
            printProperties(props);
        }
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
    public String getProperty(
            String fileName,
            String key
    ) {
        log.debug("[getProperty] - Consulta de: <{},{}>", fileName, key);
        Properties props = propertiesMap.get(fileName);
        if (props != null && props.containsKey(key)) {
            if (log.isDebugEnabled()) {
                printProperties(props);
            }
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
    private boolean isSensitiveKey(
            String key
    ) {
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
     * @throws PropertiesManagerException si el fichero no existe o falla la conversión JSON
     */
    public String exportPropertiesToJson(
            String fileName,
            boolean maskSensitiveValues
    ) {
        Properties props = propertiesMap.get(fileName);
        if (props == null) {
            String msg = String.format("No se encontró el fichero: %s", fileName + PROPERTIES_EXT);
            throw new PropertiesManagerException(msg);
        }

        Map<String, String> map = props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> {
                            String val = e.getValue().toString();
                            return maskSensitiveValues && isSensitiveKey(e.getKey().toString())
                                    ? KEY_SENSITIVE_VALUE : val;
                        }
                ));

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            String msg = "Error exportando propiedades a JSON.";
            log.error(msg, e);
            throw new PropertiesManagerException(msg, e);
        }
    }

    /**
     * Exporta todas las propiedades cargadas a formato JSON.
     * Se pueden ocultar valores sensibles.
     *
     * @param maskSensitiveValues {@code true} para ocultar valores sensibles
     * @return cadena JSON con todas las propiedades
     * @throws PropertiesManagerException si falla la conversión JSON
     */
    public String exportAllPropertiesToJson(
            boolean maskSensitiveValues
    ) {
        Map<String, Map<String, String>> allPropsMap = new HashMap<>();

        propertiesMap.forEach((fileName, props) -> {
            Map<String, String> map = props.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> {
                                String val = e.getValue().toString();
                                return maskSensitiveValues && isSensitiveKey(e.getKey().toString())
                                        ? KEY_SENSITIVE_VALUE : val;
                            }
                    ));
            allPropsMap.put(fileName, map);
        });

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allPropsMap);
        } catch (JsonProcessingException e) {
            String msg = "Error exportando todas las propiedades a JSON.";
            log.error(msg, e);
            throw new PropertiesManagerException(msg, e);
        }
    }

    /**
     * Valida que un fichero properties contenga todas las claves requeridas.
     *
     * @param fileName nombre fichero sin extensión
     * @param requiredKeys conjunto de claves obligatorias
     * @return {@code true} si todas las claves están presentes; {@code false} en caso contrario
     */
    public boolean validateRequiredKeys(
            String fileName,
            Set<String> requiredKeys
    ) {
        log.debug("[validateRequiredKeys] - fichero: {}", fileName);
        log.debug("[validateRequiredKeys] - Conjunto de Key requeridas: {}", requiredKeys);
        Properties props = propertiesMap.get(fileName);
        if (props == null) {
            log.debug("Fichero '{}' no encontrado para validación.", fileName);
            return false;
        }
        if (log.isDebugEnabled()) {
            printProperties(props);
        }
        for (String key : requiredKeys) {
            if (!props.containsKey(key)) {
                log.debug("Fichero '{}' no contiene la clave requerida: {}", fileName, key);
                return false;
            }
        }
        return true;
    }

    /**
     * Recarga todas las propiedades desde el directorio configurado en Constantes.CONFIG_DIR,
     * actualizando el mapa interno de forma sincronizada.
     */
    public synchronized void reload(

    ) {
        log.debug("Recargando propiedades...");
        loadAllProperties(this.configDir);
    }

    /**
     * Extrae el nombre base de un fichero sin la extensión.
     *
     * @param fileName nombre del fichero con extensión
     * @return nombre sin extensión
     */
    private String stripExtension(
            String fileName
    ) {
        log.debug("[stripExtension]");
        if (fileName == null) {
            log.debug("[stripExtension] - El fichero es null.");
            return "";
        }
        log.debug("[stripExtension] - Eliminación de la extensión para el fichero: {}", fileName);
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            log.debug("[stripExtension] - El fichero no tiene extensión: {}", fileName);
            return fileName;
        }
        String nombreSinExtension =  fileName.substring(0, lastDot);
        log.debug("[stripExtension] - Nombre del fichero sin extensión: {}", nombreSinExtension);
        return nombreSinExtension;
    }

    /**
     * Impresión de ficheros properties
     * @param props El propertie a imprimir
     */
    public void printProperties(
            Properties props
    ) {
        props.forEach((key, value) -> {
            String val = isSensitiveKey(key.toString()) ? KEY_SENSITIVE_VALUE : value.toString();
            log.debug("{} = {}", key, val);
        });
    }

    /**
     * Establece la ruta de configuración para cargar ficheros .properties.
     *
     * @param configDir ruta del directorio
     */
    public void setConfigDir(String configDir) {
        this.configDir = (configDir == null || configDir.isBlank()) ? DEFAULT_CONFIG_DIR : configDir;
        log.debug("Ruta de configuración establecida: {}", this.configDir);
    }

    /**
     * Establece la clave secreta usada para desencriptar valores sensibles.
     *
     * @param key clave secreta
     */
    public void setSecretKey(String key) {
        this.secretKey = (key == null || key.isBlank()) ? DEFAULT_SECRET_KEY : key;
        log.debug("Clave secreta establecida (oculta en log)");
    }
}

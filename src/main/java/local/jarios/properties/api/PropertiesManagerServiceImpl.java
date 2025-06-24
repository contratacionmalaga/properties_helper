package local.jarios.properties.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.common.util.Constantes;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión centralizada de ficheros .properties.
 *
 * <p>Esta implementación proporciona funcionalidades avanzadas para la gestión de archivos
 * de configuración .properties, incluyendo carga desde directorios externos o classpath,
 * manejo de valores sensibles, exportación a JSON y validación de configuraciones.</p>
 *
 * <p>Implementa el patrón Singleton thread-safe para garantizar acceso global único
 * y gestión consistente del estado de las propiedades cargadas.</p>
 *
 * <h2>Características principales:</h2>
 * <ul>
 *   <li>Carga automática de múltiples ficheros .properties con fallback</li>
 *   <li>Inmutabilidad de propiedades para prevenir modificaciones accidentales</li>
 *   <li>Sistema de ocultamiento de valores sensibles configurable</li>
 *   <li>Exportación flexible a formato JSON</li>
 *   <li>Validación de claves requeridas por configuración</li>
 *   <li>Recarga sincronizada y thread-safe</li>
 *   <li>Búsqueda jerárquica: properties → variables entorno → propiedades sistema</li>
 * </ul>
 *
 * <h2>Uso típico:</h2>
 * <pre>{@code
 * PropertiesManagerService manager = PropertiesManagerServiceImpl.getInstance();
 * manager.setConfigDir("/path/to/config");
 * manager.setSensitiveKeys(Set.of("password", "token", "secret"));
 * manager.loadAllProperties();
 *
 * String value = manager.getProperty("database", "connection.url");
 * }</pre>
 *
 * @author Juan Antonio
 * @version 2.0
 * @since 2024-06-18
 * @see PropertiesManagerService
 * @see PropertiesManagerException
 */
@Slf4j
public class PropertiesManagerServiceImpl implements PropertiesManagerService {

    /**
     * Instancia única (singleton) del gestor de propiedades.
     * Inicialización temprana y thread-safe mediante static final.
     */
    private static final PropertiesManagerServiceImpl INSTANCE = new PropertiesManagerServiceImpl();

    /**
     * Convertidor JSON utilizado para exportación de propiedades.
     * Configurado con pretty printing por defecto.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Directorio actual desde el que se cargan los archivos .properties.
     * Por defecto utiliza {@link Constantes#DEFAULT_CONFIG_DIR}.
     */
    private volatile String configDir;

    /**
     * Clave secreta utilizada para operaciones de desencriptación.
     * Por defecto utiliza {@link Constantes#DEFAULT_SECRET_KEY}.
     */
    private volatile String secretKey;

    /**
     * Mapa inmutable con las propiedades cargadas.
     * La clave es el nombre del fichero sin extensión, el valor son las Properties inmutables.
     * Nota de implementación: Se utiliza un mapa inmutable para garantizar thread-safety en operaciones de lectura
     */
    private volatile Map<String, Properties> propertiesMap = Collections.emptyMap();

    /**
     * Conjunto de claves consideradas sensibles que deben ser ocultadas.
     * Las comparaciones se realizan sin distinción de mayúsculas/minúsculas.
     */
    private volatile Set<String> sensitiveKeys = Collections.emptySet();

    /**
     * Constructor privado para implementación del patrón Singleton.
     * Inicializa la instancia con valores por defecto definidos en {@link Constantes}.
     */
    private PropertiesManagerServiceImpl() {
        this.configDir = Constantes.DEFAULT_CONFIG_DIR;
        this.secretKey = Constantes.DEFAULT_SECRET_KEY;
        log.debug("[PropertiesManagerServiceImpl] - PropertiesManagerService inicializado con directorio: {} y clave secreta por defecto", configDir);
    }

    /**
     * Obtiene la instancia única del servicio de gestión de propiedades.
     *
     * @return La instancia singleton como interfaz {@link PropertiesManagerService}
     * @since 1.0
     */
    public static PropertiesManagerService getInstance() {
        log.trace("[setSensitiveKeys] - Solicitada instancia singleton de PropertiesManagerService");
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las claves sensibles se almacenan en un conjunto inmutable y se utilizan
     * para comparaciones case-insensitive durante las operaciones de impresión y exportación.</p>
     *
     * @param keys {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public void setSensitiveKeys(Set<String> keys) throws PropertiesManagerException {

        log.debug("[setSensitiveKeys] -");

        try {
            this.sensitiveKeys = (keys == null) ? Collections.emptySet() : Set.copyOf(keys);
            log.debug("[setSensitiveKeys] - Configuradas {} claves sensibles para ocultamiento", this.sensitiveKeys.size());
            log.debug("[setSensitiveKeys] - Claves sensibles establecidas: {}", this.sensitiveKeys);
        } catch (Exception e) {
            String errorMsg = String.format("[setSensitiveKeys] - Error al establecer claves sensibles. Error: %s", e.getMessage());
            log.error(errorMsg, e);
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Obtiene el conjunto de claves sensibles definidas.</p>
     *
     * @return Conjunto de claves sensibles
     */
    @Override
    public Set<String> getSensitiveKeys() {

        log.debug("[getSensitiveKeys] -");

        return this.sensitiveKeys;
    }

    /**
     * {@inheritDoc}
     *
     * <p>La carga se realiza de forma sincronizada para garantizar thread-safety.
     * Los archivos se procesan desde el directorio configurado, y en caso de error
     * en algún archivo individual, se continúa con el resto sin interrumpir el proceso.</p>
     *
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public synchronized void loadAllProperties() throws PropertiesManagerException {

        String dirPath = getConfigDir();
        log.debug("[loadAllProperties] - Determino el directorio de configuración (defindo | por defecto): {}", dirPath);

        Map<String, Properties> tempMap = new HashMap<>();
        log.debug("[loadAllProperties] - Creado el objeto Map<String, Properties> correctamente");

        File configDirectory = new File(dirPath);
        log.debug("[loadAllProperties] - Creado el objeto File. Ruta absoluta: {}", configDirectory.getAbsolutePath());

        try {
            log.debug("[loadAllProperties] - Verificando directorio: {} (ruta absoluta: {})", dirPath, configDirectory.getAbsolutePath());

            if (!configDirectory.exists()) {
                log.debug("[loadAllProperties] - El directorio de configuración no existe.");

                propertiesMap = Collections.emptyMap();
                log.debug("[loadAllProperties] - Establezco propertiesMap = Collections.emptyMap().");
                return;
            }

            if (!configDirectory.isDirectory()) {
                throw new PropertiesManagerException("[loadAllProperties] - La ruta especificada no es un directorio: " + dirPath);
            }

            log.debug("[loadAllProperties] - El directorio de configuración existe.");

            File[] propertyFiles = configDirectory.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(Constantes.PROPERTIES_EXT));

            if (propertyFiles == null || propertyFiles.length == 0) {
                log.warn("[loadAllProperties] - No se encontraron archivos .properties en el directorio: {}", dirPath);
                propertiesMap = Collections.emptyMap();
                return;
            }

            log.debug("[loadAllProperties] - Encontrados {} archivos .properties para procesar", propertyFiles.length);

            int loadedCount = 0;
            int errorCount = 0;

            for (File file : propertyFiles) {
                try {
                    log.debug("[loadAllProperties] - Procesando archivo: {}", file.getName());

                    Properties props = loadPropertiesFromFile(file);
                    log.debug("[loadAllProperties] - Cargadas todas las propiedades asociada al fichero: {}", file.getName());

                    String fileName = stripExtension(file.getName());
                    log.debug("[loadAllProperties] - Eliminación de la extensión al fichero: {}. Fichero sin extensión: {}", file.getName(), fileName);

                    tempMap.put(fileName, props);
                    log.debug("[loadAllProperties] - Almacenados los datos en Map<String, Properties> correctamente.");

                    log.debug("[loadAllProperties] - Nº de ficheros cargados: {}", loadedCount);
                    loadedCount++;

                } catch (Exception e) {
                    errorCount++;
                    log.error("[loadAllProperties] - Error cargando archivo: {} - {}", file.getName(), e.getMessage());
                }
            }

            propertiesMap = Collections.unmodifiableMap(tempMap);
            log.debug("[loadAllProperties] - Almacenada la información en el objeto propertiesMap.");

            log.debug("[loadAllProperties] - Carga completada: {} archivos cargados, {} errores, {} propiedades totales",
                    loadedCount, errorCount, propertiesMap.size());

        } catch (UnsupportedOperationException e) {
            String errorMsg = "[loadAllProperties] - Error crítico durante la carga de propiedades desde: " + dirPath + ". Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fileNameWithoutExtension {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public void printProperties(String fileNameWithoutExtension) throws PropertiesManagerException {

        validateFileName(fileNameWithoutExtension);

        try {
            Properties props = propertiesMap.get(fileNameWithoutExtension);
            if (props == null) {
                throw new PropertiesManagerException(
                        String.format("Archivo no encontrado: %s%s", fileNameWithoutExtension, Constantes.PROPERTIES_EXT));
            }

            log.debug("=== Propiedades de {} ===", fileNameWithoutExtension + Constantes.PROPERTIES_EXT);
            printPropertiesInternal(props);

        } catch (PropertiesManagerException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "Error imprimiendo propiedades del archivo: " + fileNameWithoutExtension + ". Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public List<String> getListFiles() throws PropertiesManagerException {

        List<String> listFicheros = new ArrayList<>();

        if (propertiesMap.isEmpty()) {
            log.debug("[getListFiles] - No hay archivos .properties cargados para mostrar");
            return listFicheros;
        }

        listFicheros = new ArrayList<>(propertiesMap.keySet());
        log.debug("[getListFiles] - Devuelvo la lista: {}", listFicheros);
        return new ArrayList<>(propertiesMap.keySet());
    }

    /**
     * {@inheritDoc}
     *
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public void printAllProperties() throws PropertiesManagerException {
        try {
            if (propertiesMap.isEmpty()) {
                log.debug("No hay archivos .properties cargados para mostrar");
                return;
            }

            log.debug("=== Imprimiendo todas las propiedades ({} archivos) ===", propertiesMap.size());
            propertiesMap.forEach((fileName, props) -> {
                try {
                    log.debug("--- Archivo: {} ---", fileName + Constantes.PROPERTIES_EXT);
                    printPropertiesInternal(props);
                } catch (Exception e) {
                    log.error("Error imprimiendo propiedades del archivo: {}. Error: {}", fileName, e.getMessage());
                }
            });

        } catch (Exception e) {
            String errorMsg = "Error imprimiendo todas las propiedades. Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fileNameWithoutExtension {@inheritDoc}
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public Properties getProperties(String fileNameWithoutExtension) throws PropertiesManagerException {

        validateFileName(fileNameWithoutExtension);
        log.debug("[getProperties] - Filename váldio: {}", fileNameWithoutExtension);

        try {

            Properties props = propertiesMap.get(fileNameWithoutExtension);

            if (props == null) {
                log.debug("[getProperties] - Archivo no encontrado. Devuelvo new Properties.");
                return new Properties();
            }

            log.debug("[getProperties] - Properties: {}", props);
            return props;

        } catch (Exception ex) {

            String errorMsg = String.format("Error obteniendo propiedades del archivo: %s. Error: %s", fileNameWithoutExtension, ex.getMessage());
            log.error(errorMsg, ex);
            throw new PropertiesManagerException(errorMsg, ex);

        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>La búsqueda se realiza en el siguiente orden de prioridad:</p>
     * <ol>
     *   <li>Propiedades del archivo especificado</li>
     *   <li>Variables de entorno del sistema</li>
     *   <li>Propiedades del sistema Java</li>
     * </ol>
     *
     * @param fileName {@inheritDoc}
     * @param key      {@inheritDoc}
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public String getProperty(String fileName, String key) throws PropertiesManagerException {
        validateFileName(fileName);
        validateKey(key);

        try {
            log.debug("Buscando propiedad: archivo='{}', clave='{}'", fileName, key);

            // 1. Buscar en las propiedades del archivo
            Properties props = propertiesMap.get(fileName);
            if (props != null && props.containsKey(key)) {
                String value = props.getProperty(key);
                log.debug("Valor encontrado en archivo de propiedades: {}='{}'", key,
                        isSensitiveKey(key) ? Constantes.KEY_SENSITIVE_VALUE : value);
                return value;
            }

            // 2. Buscar en variables de entorno
            String envValue = System.getenv(key);
            if (envValue != null) {
                log.debug("Valor encontrado en variables de entorno: {}='{}'", key,
                        isSensitiveKey(key) ? Constantes.KEY_SENSITIVE_VALUE : envValue);
                return envValue;
            }

            // 3. Buscar en propiedades del sistema
            String sysValue = System.getProperty(key);
            if (sysValue != null) {
                log.debug("Valor encontrado en propiedades del sistema: {}='{}'", key,
                        isSensitiveKey(key) ? Constantes.KEY_SENSITIVE_VALUE : sysValue);
                return sysValue;
            }

            log.debug("Clave '{}' no encontrada en ninguna fuente para archivo '{}'", key, fileName);
            return null;

        } catch (Exception e) {
            String errorMsg = String.format("Error obteniendo propiedad: archivo='%s', clave='%s'", fileName, key);
            log.error(errorMsg, e);
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public Map<String, Properties> getAllProperties() throws PropertiesManagerException {
        try {
            log.debug("[getAllProperties] - Devolviendo todas las propiedades ({} archivos cargados)", propertiesMap.size());
            return propertiesMap;
        } catch (Exception e) {
            String errorMsg = "Error obteniendo todas las propiedades";
            log.error(errorMsg, e);
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fileName            {@inheritDoc}
     * @param maskSensitiveValues {@inheritDoc}
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public String exportPropertiesToJson(String fileName, boolean maskSensitiveValues)
            throws PropertiesManagerException {
        validateFileName(fileName);

        try {
            Properties props = propertiesMap.get(fileName);
            if (props == null) {
                throw new PropertiesManagerException(
                        String.format("Archivo no encontrado para exportación: %s%s", fileName, Constantes.PROPERTIES_EXT));
            }

            log.debug("Exportando a JSON archivo: {} (máscara sensibles: {})", fileName, maskSensitiveValues);

            Map<String, String> exportMap = props.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> {
                                String value = e.getValue().toString();
                                return maskSensitiveValues && isSensitiveKey(e.getKey().toString())
                                        ? Constantes.KEY_SENSITIVE_VALUE : value;
                            }
                    ));

            String json = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(exportMap);
            log.debug("JSON generado exitosamente para archivo: {} ({} propiedades)", fileName, exportMap.size());
            return json;

        } catch (JsonProcessingException e) {
            String errorMsg = "Error generando JSON para archivo: " + fileName + ". Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Error exportando propiedades a JSON: " + fileName + ". Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param maskSensitiveValues {@inheritDoc}
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public String exportAllPropertiesToJson(boolean maskSensitiveValues) throws PropertiesManagerException {
        try {
            log.debug("Exportando todas las propiedades a JSON (máscara sensibles: {})", maskSensitiveValues);

            Map<String, Map<String, String>> allPropsMap = new HashMap<>();

            propertiesMap.forEach((fileName, props) -> {
                Map<String, String> filePropsMap = props.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey().toString(),
                                e -> {
                                    String value = e.getValue().toString();
                                    return maskSensitiveValues && isSensitiveKey(e.getKey().toString())
                                            ? Constantes.KEY_SENSITIVE_VALUE : value;
                                }
                        ));
                allPropsMap.put(fileName, filePropsMap);
            });

            String json = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(allPropsMap);
            log.debug("JSON generado exitosamente para todos los archivos ({} archivos, máscara: {})",
                    allPropsMap.size(), maskSensitiveValues);
            return json;

        } catch (JsonProcessingException e) {
            String errorMsg = "Error generando JSON para todas las propiedades. " + ". Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Error exportando todas las propiedades a JSON. " + ". Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fileName     {@inheritDoc}
     * @param requiredKeys {@inheritDoc}
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public boolean validateRequiredKeys(String fileName, Set<String> requiredKeys)
            throws PropertiesManagerException {
        validateFileName(fileName);

        try {
            if (requiredKeys == null || requiredKeys.isEmpty()) {
                log.debug("No hay claves requeridas para validar en archivo: {}", fileName);
                return true;
            }

            Properties props = propertiesMap.get(fileName);
            if (props == null) {
                log.debug("Archivo no encontrado para validación: {}", fileName);
                return false;
            }

            log.debug("Validando {} claves requeridas en archivo: {}", requiredKeys.size(), fileName);

            Set<String> missingKeys = new HashSet<>();
            for (String key : requiredKeys) {
                if (!props.containsKey(key)) {
                    missingKeys.add(key);
                }
            }

            if (missingKeys.isEmpty()) {
                log.debug("Validación exitosa: todas las claves requeridas están presentes en archivo: {}", fileName);
                return true;
            } else {
                log.debug("Validación fallida en archivo '{}': claves faltantes: {}", fileName, missingKeys);
                return false;
            }

        } catch (Exception e) {
            String errorMsg = "Error validando claves requeridas en archivo: " + fileName + ". Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 1.0
     */
    @Override
    public synchronized void reload() throws PropertiesManagerException {
        try {
            log.debug("Iniciando recarga de todas las propiedades");
            Map<String, Properties> previousMap = propertiesMap;
            loadAllProperties();
            log.debug("Recarga completada: {} archivos previamente cargados → {} archivos actuales",
                    previousMap.size(), propertiesMap.size());
        } catch (Exception e) {
            String errorMsg = "Error durante la recarga de propiedades. Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param configDir {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public void setConfigDir(String configDir) throws PropertiesManagerException {
        try {
            if (configDir == null || configDir.isBlank()) {
                this.configDir = Constantes.DEFAULT_CONFIG_DIR;
                log.debug("[setConfigDir] - Directorio de configuración establecido a valor por defecto: {}", this.configDir);
            } else {
                this.configDir = configDir.trim();
                log.debug("[setConfigDir] - Directorio de configuración establecido: {}", this.configDir);
            }
        } catch (Exception e) {
            String errorMsg = "[setConfigDir] - Error estableciendo directorio de configuración: %s. Error: %s" + configDir;
            log.error(errorMsg, e);
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public String getConfigDir() throws PropertiesManagerException {
        try {
            String currentDir = (this.configDir == null || this.configDir.isBlank())
                    ? Constantes.DEFAULT_CONFIG_DIR : this.configDir;
            log.debug("[getConfigDir] - Directorio de configuración definido: {}", currentDir);
            return currentDir;
        } catch (Exception e) {
            String errorMsg = "[getConfigDir] - Error obteniendo directorio de configuración. Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param key {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public void setSecretKey(String key) throws PropertiesManagerException {
        try {
            if (key == null || key.isBlank()) {
                this.secretKey = Constantes.DEFAULT_SECRET_KEY;
                log.debug("Clave secreta establecida a valor por defecto");
            } else {
                this.secretKey = key.trim();
                log.debug("Clave secreta establecida (longitud: {} caracteres)", key.length());
            }
        } catch (Exception e) {
            String errorMsg = "Error estableciendo clave secreta. Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public String getSecretKey() throws PropertiesManagerException {
        try {
            String currentKey = (this.secretKey == null || this.secretKey.isBlank())
                    ? Constantes.DEFAULT_SECRET_KEY : this.secretKey;
            log.trace("Devolviendo clave secreta (longitud: {} caracteres)", currentKey.length());
            return currentKey;
        } catch (Exception e) {
            String errorMsg = "Error obteniendo clave secreta. Error: {}";
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las propiedades se almacenan de forma inmutable y thread-safe.
     * Si el archivo ya existía, sus propiedades anteriores son completamente reemplazadas.</p>
     *
     * @param fileName   {@inheritDoc}
     * @param properties {@inheritDoc}
     * @throws PropertiesManagerException {@inheritDoc}
     * @since 2.0
     */
    @Override
    public synchronized void addProperties(String fileName, Properties properties)
            throws PropertiesManagerException {
        validateFileName(fileName);
        if (properties == null) {
            String msg = "[addProperties] - El objeto properties es nulo.";
            log.debug(msg);
            throw new PropertiesManagerException(msg);
        }

        try {
            // Crear nuevo mapa inmutable con las propiedades actualizadas
            Map<String, Properties> newMap = new HashMap<>(this.propertiesMap);
            Properties previousProps = newMap.put(fileName, properties);

            this.propertiesMap = Collections.unmodifiableMap(newMap);

            if (previousProps != null) {
                log.debug("Propiedades reemplazadas para archivo '{}': {} propiedades anteriores → {} nuevas",
                        fileName, previousProps.size(), properties.size());
            } else {
                log.debug("Propiedades añadidas para nuevo archivo '{}': {} propiedades",
                        fileName, properties.size());
            }

        } catch (UnsupportedOperationException e) {
            String errorMsg = "Error añadiendo propiedades para archivo: " + fileName;
            log.error(errorMsg, e.getMessage());
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    // ================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // ================================

    /**
     * Carga propiedades desde un archivo del sistema de archivos.
     *
     * @param file El archivo .properties a cargar
     * @return Properties cargadas del archivo
     * @throws IllegalArgumentException Si el archivo es nulo, no existe, no es un archivo válido o no es legible
     */
    private Properties loadPropertiesFromFile(File file) {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            throw new IllegalArgumentException("[loadPropertiesFromFile] - Archivo inválido o no legible: " +
                    (file != null ? file.getAbsolutePath() : "null"));
        }

        log.debug("[loadPropertiesFromFile] - Cargando propiedades desde archivo: {} (tamaño: {} bytes)",
                file.getName(), file.length());

        Properties props = new Properties();
        log.debug("[loadPropertiesFromFile] - Creación de un objeto Properties correctamente.");

        try (InputStream inputStream = new FileInputStream(file);
             BufferedInputStream bufferedStream = new BufferedInputStream(inputStream)) {

            props.load(bufferedStream);
            log.debug("[loadPropertiesFromFile] - Archivo cargado exitosamente: {} ({} propiedades)", file.getName(), props.size());
            return props;

        } catch (IOException e) {
            String errorMsg = String.format("[loadPropertiesFromFile] - Error cargando archivo: %s. Error: %s", file.getName(), e.getMessage());
            log.error(errorMsg);
            throw new PropertiesManagerException(errorMsg, e);
        }
    }

    /**
     * Imprime las propiedades proporcionadas en el log, aplicando enmascaramiento
     * a los valores de claves sensibles según la configuración actual.
     *
     * <p>Si el objeto Properties es nulo o está vacío, registra un mensaje informativo
     * y termina la ejecución. Para cada propiedad válida, verifica si la clave es
     * sensible y en ese caso oculta el valor utilizando el valor de enmascaramiento
     * definido en las constantes.</p>
     *
     * @param props el objeto Properties a imprimir, puede ser {@code null} o vacío
     */
    private void printPropertiesInternal(Properties props) {
        if (props == null || props.isEmpty()) {
            log.debug("[printPropertiesInternal] - No properties to display");
            return;
        }

        props.forEach((key, value) -> {
            String displayValue = isSensitiveKey(key.toString())
                    ? Constantes.KEY_SENSITIVE_VALUE
                    : value.toString();
            log.info("  - {} = {}", key, displayValue);
        });
    }

    /**
     * Determina si una clave debe considerarse sensible basándose en la configuración
     * actual de claves sensibles.
     *
     * <p>La evaluación se realiza comparando la clave proporcionada (convertida a
     * minúsculas) con cada una de las claves sensibles configuradas. Si cualquiera
     * de las claves sensibles está contenida en la clave evaluada, se considera
     * sensible. La comparación es case-insensitive.</p>
     *
     * @param key la clave a evaluar, puede ser {@code null} o vacía
     * @return {@code true} si la clave es considerada sensible, {@code false} en caso contrario
     */
    private boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        String keyLower = key.toLowerCase(Locale.ROOT);
        return sensitiveKeys.stream()
                .anyMatch(sensitive -> keyLower.contains(sensitive.toLowerCase(Locale.ROOT)));
    }

    /**
     * Extrae el nombre base de un archivo eliminando su extensión.
     *
     * <p>Busca el último punto (.) en el nombre del archivo y devuelve la parte
     * anterior a este punto. Si no encuentra ningún punto, devuelve el nombre
     * completo del archivo sin modificaciones.</p>
     *
     * @param fileName el nombre del archivo del cual extraer la extensión
     * @return el nombre del archivo sin extensión
     * @throws IllegalArgumentException si {@code fileName} es {@code null} o está vacío
     */
    private String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new PropertiesManagerException("[stripExtension] File name cannot be null or blank");
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            log.debug("[stripExtension] - File '{}' has no extension", fileName);
            return fileName;
        }

        String nameWithoutExtension = fileName.substring(0, lastDot);
        log.debug("[stripExtension] - Stripped extension from '{}' -> '{}'", fileName, nameWithoutExtension);
        return nameWithoutExtension;
    }

    /**
     * Valida que el nombre de archivo proporcionado sea válido para su uso
     * en las operaciones del gestor de propiedades.
     *
     * <p>Un nombre de archivo válido no puede ser {@code null}, vacío o contener
     * solo espacios en blanco.</p>
     *
     * @param fileName el nombre del archivo a validar
     * @throws PropertiesManagerException si el nombre del archivo es inválido
     */
    private void validateFileName(String fileName) throws PropertiesManagerException {
        if (fileName == null || fileName.isBlank()) {
            String msg = "[validateFileName] - El nombre del archivo no puede ser null o vacío";
            log.error(msg);
            throw new PropertiesManagerException(msg);
        }
    }

    /**
     * Valida que la clave de propiedad proporcionada sea válida para su uso
     * en las operaciones de búsqueda y manipulación de propiedades.
     *
     * <p>Una clave válida no puede ser {@code null}, vacía o contener solo
     * espacios en blanco.</p>
     *
     * @param key la clave de propiedad a validar
     * @throws PropertiesManagerException si la clave es inválida
     */
    private void validateKey(String key) throws PropertiesManagerException {
        if (key == null || key.isBlank()) {
            String msg = String.format("[validateKey] - %s es null o blanck", key);
            log.error(msg);
            throw new PropertiesManagerException(msg);
        } else {
            log.debug("[validateKey] - El valor de la Key '{}' es correcto.", key);
        }
    }
}
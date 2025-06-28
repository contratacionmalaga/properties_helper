package local.jarios.properties.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.jarios.properties.common.util.Constantes;
import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.helpers.FileHelper;
import local.jarios.properties.helpers.MapHelper;
import local.jarios.properties.helpers.PropertiesHelper;
import local.jarios.properties.helpers.StringHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PropertiesManagerServiceImpl implements PropertiesManagerService {

    private static final Logger LOGGER = LogManager.getLogger(PropertiesManagerServiceImpl.class);
    private static final PropertiesManagerServiceImpl INSTANCE = new PropertiesManagerServiceImpl();
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private volatile String configDir = Constantes.DEFAULT_CONFIG_DIR;
    private volatile String secretKey = Constantes.DEFAULT_SECRET_KEY;
    private volatile Map<String, Properties> propertiesMap = Collections.emptyMap();
    private volatile Set<String> sensitiveKeys = Collections.emptySet();

    private PropertiesManagerServiceImpl() { /* Singleton */ }

    public static PropertiesManagerService getInstance() {
        return INSTANCE;
    }

    // ============================================================
    //                  <<< Public API Methods >>>
    // ============================================================

    @Override
    public void setConfigDir(String configDir) throws PropertiesManagerException {
        LOGGER.debug("[setConfigDir] - Inicio.");
        this.configDir = (configDir == null || configDir.isBlank())
                ? Constantes.DEFAULT_CONFIG_DIR
                : configDir.trim();
        LOGGER.debug("[setConfigDir] - Nueva configuración: {}", this.configDir);
    }

    @Override
    public String getConfigDir() {
        LOGGER.debug("[getConfigDir] - Inicio.");
        return (configDir == null || configDir.isBlank()) ? Constantes.DEFAULT_CONFIG_DIR : configDir;
    }

    @Override
    public void setSecretKey(String key) {
        LOGGER.debug("[setSecretKey] - Inicio.");
        this.secretKey = (key == null || key.isBlank()) ? Constantes.DEFAULT_SECRET_KEY : key.trim();
        LOGGER.debug("[setSecretKey] - Se estableció la clave (longitud: {})", this.secretKey.length());
    }

    @Override
    public String getSecretKey() {
        LOGGER.debug("[getSecretKey] - Inicio.");
        return (secretKey == null || secretKey.isBlank()) ? Constantes.DEFAULT_SECRET_KEY : secretKey;
    }

    @Override
    public synchronized void setSensitiveKeys(Set<String> keys) {
        LOGGER.debug("[setSensitiveKeys] - Inicio.");
        this.sensitiveKeys = (keys == null) ? Collections.emptySet() : Set.copyOf(keys);
        LOGGER.debug("[setSensitiveKeys] - Claves sensibles actualizadas: {}", this.sensitiveKeys);
    }

    @Override
    public Set<String> getSensitiveKeys() {
        LOGGER.debug("[getSensitiveKeys] - {}", sensitiveKeys);
        return sensitiveKeys;
    }

    @Override
    public synchronized void loadAllProperties() throws PropertiesManagerException {
        LOGGER.debug("[loadAllProperties] - Inicio.");
        File dir = new File(getConfigDir());
        validateDirectory(dir);

        Map<String, Properties> temp = new HashMap<>();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(Constantes.PROPERTIES_EXT));

        if (files == null || files.length == 0) {
            LOGGER.warn("[loadAllProperties] - No se encontraron .properties en {}", dir.getAbsolutePath());
            this.propertiesMap = Collections.emptyMap();
            return;
        }

        for (File f : files) {
            try {
                Properties p = loadPropertiesFromFile(f);
                String key = stripExtension(f.getName());
                temp.put(key, p);
                LOGGER.debug("[loadAllProperties] - Archivo cargado: {} ({} propiedades)", f.getName(), p.size());
            } catch (PropertiesManagerException e) {
                LOGGER.error("[loadAllProperties] - Error en archivo {}: {}", f.getName(), e.getMessage());
            }
        }

        this.propertiesMap = Collections.unmodifiableMap(temp);
        LOGGER.debug("[loadAllProperties] - Total archivos cargados: {}", propertiesMap.size());
    }

    @Override
    public synchronized void reload() throws PropertiesManagerException {
        LOGGER.debug("[reload] - Inicio.");
        loadAllProperties();
    }

    @Override
    public void addProperties(String fileName, Properties props) throws PropertiesManagerException {
        LOGGER.debug("[addProperties] - Inicio para {}", fileName);
        validateFileName(fileName);
        validateProperties(props);

        Map<String, Properties> newMap = new HashMap<>(propertiesMap);
        Properties prev = newMap.put(fileName, props);
        propertiesMap = Collections.unmodifiableMap(newMap);
        LOGGER.debug(prev == null
                        ? "[addProperties] - Añadido '{}', {} propiedades"
                        : "[addProperties] - Reemplazado '{}', antes {} propiedades, ahora {}",
                fileName, props.size(), prev == null ? 0 : prev.size());
    }

    @Override
    public void printProperties(String fileName) throws PropertiesManagerException {
        LOGGER.debug("[printProperties] - Inicio para {}", fileName);
        validateFileName(fileName);
        validateMap();

        Properties props = propertiesMap.get(fileName);
        if (props == null) {
            throw new PropertiesManagerException("printProperties: archivo no encontrado -> " + fileName);
        }

        printPropertiesInternal(props);
    }

    @Override
    public void printAllProperties() throws PropertiesManagerException {
        LOGGER.debug("[printAllProperties] - Inicio.");
        validateMap();
        propertiesMap.forEach((k, props) -> {
            LOGGER.info("=== {}{} ===", k, Constantes.PROPERTIES_EXT);
            try {
                printPropertiesInternal(props);
            } catch (PropertiesManagerException e) {
                LOGGER.error("[printAllProperties] - Error en {}: {}", k, e.getMessage());
            }
        });
    }

    @Override
    public Properties getProperties(String fileName) throws PropertiesManagerException {
        LOGGER.debug("[getProperties] - Inicio para {}", fileName);
        validateFileName(fileName);
        validateMap();

        return Optional.ofNullable(propertiesMap.get(fileName))
                .map(props -> {
                    LOGGER.debug("[getProperties] - {} propiedades recuperadas", props.size());
                    return props;
                })
                .orElseGet(Properties::new);
    }

    @Override
    public String getProperty(String fileName, String key) throws PropertiesManagerException {
        LOGGER.debug("[getProperty] - Inicio para {} :: {}", fileName, key);
        validateFileName(fileName);
        validateKey(key);
        validateMap();

        Properties props = propertiesMap.get(fileName);
        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        }

        String env = System.getenv(key);
        if (env != null) return env;
        return System.getProperty(key);  // returns null if not set
    }

    @Override
    public List<String> getListFiles() throws PropertiesManagerException {
        LOGGER.debug("[getListFiles] - Inicio.");
        validateMap();
        return new ArrayList<>(propertiesMap.keySet());
    }

    @Override
    public Map<String, Properties> getAllProperties() throws PropertiesManagerException {
        LOGGER.debug("[getAllProperties] - {}", propertiesMap.size());
        validateMap();
        return propertiesMap;
    }

    @Override
    public boolean validateRequiredKeys(String fileName, Set<String> requiredKeys) throws PropertiesManagerException {
        LOGGER.debug("[validateRequiredKeys] - Inicio en {}", fileName);
        validateFileName(fileName);
        validateMap();

        if (requiredKeys == null || requiredKeys.isEmpty()) return true;

        Properties props = propertiesMap.get(fileName);
        if (props == null) return false;

        Set<String> missing = requiredKeys.stream()
                .filter(k -> !props.containsKey(k))
                .collect(Collectors.toSet());

        return missing.isEmpty();
    }

    @Override
    public String exportPropertiesToJson(String fileName, boolean maskSensitive) throws PropertiesManagerException {
        LOGGER.debug("[exportPropertiesToJson] - Inicio para {}", fileName);
        validateFileName(fileName);
        validateMap();

        Properties props = propertiesMap.get(fileName);
        if (props == null) throw new PropertiesManagerException("exportPropertiesToJson: archivo no encontrado -> " + fileName);

        Map<String, String> map = props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> maskSensitive && isSensitiveKey(e.getKey().toString())
                                ? Constantes.KEY_SENSITIVE_VALUE
                                : e.getValue().toString()
                ));

        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new PropertiesManagerException("Error exportando a JSON", e);
        }
    }

    @Override
    public String exportAllPropertiesToJson(boolean maskSensitive) throws PropertiesManagerException {
        LOGGER.debug("[exportAllPropertiesToJson] - Inicio.");
        validateMap();

        Map<String, Map<String, String>> all = new HashMap<>();
        propertiesMap.forEach((k, props) -> all.put(k, props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> maskSensitive && isSensitiveKey(e.getKey().toString())
                                ? Constantes.KEY_SENSITIVE_VALUE
                                : e.getValue().toString()
                ))
        ));

        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(all);
        } catch (JsonProcessingException e) {
            throw new PropertiesManagerException("Error exportando todas las propiedades a JSON", e);
        }
    }

    // ============================================================
    //                  <<< Private Helpers >>>
    // ============================================================

    private Properties loadPropertiesFromFile(File file) throws PropertiesManagerException {
        LOGGER.debug("[loadPropertiesFromFile] - Inicio para {}", file.getAbsolutePath());
        validateFile(file);

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new PropertiesManagerException("Error cargando archivo " + file.getName(), e);
        }
    }

    private void printPropertiesInternal(Properties props) throws PropertiesManagerException {
        LOGGER.debug("[printPropertiesInternal] - Inicio.");
        validateProperties(props);

        if (props.isEmpty()) {
            LOGGER.info("[printPropertiesInternal] - No hay propiedades para imprimir.");
            return;
        }

        props.forEach((k, v) -> {
            String val = isSensitiveKey(k.toString()) ? Constantes.KEY_SENSITIVE_VALUE : v.toString();
            LOGGER.info("{} = {}", k, val);
        });
    }

    private boolean isSensitiveKey(String key) {
        if (StringHelper.isStringInvalid(key)) return false;
        String lower = key.toLowerCase(Locale.ROOT);
        return sensitiveKeys.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(lower::contains);
    }

    private String stripExtension(String name) throws PropertiesManagerException {
        validateFileName(name);
        int idx = name.lastIndexOf('.');
        return (idx == -1) ? name : name.substring(0, idx);
    }

    private void validateMap() throws PropertiesManagerException {
        if (MapHelper.isMapInvalid(propertiesMap)) {
            throw new PropertiesManagerException("Map no válido: " + propertiesMap);
        }
    }

    private void validateFileName(String name) throws PropertiesManagerException {
        if (StringHelper.isStringInvalid(name)) {
            throw new PropertiesManagerException("Nombre de archivo inválido: " + name);
        }
    }

    private void validateKey(String key) throws PropertiesManagerException {
        if (StringHelper.isStringInvalid(key)) {
            throw new PropertiesManagerException("Clave inválida: " + key);
        }
    }

    private void validateFile(File file) throws PropertiesManagerException {
        if (FileHelper.isInvalidFile(file)) {
            throw new PropertiesManagerException("Archivo inválido o no legible: " + file);
        }
    }

    private void validateDirectory(File dir) throws PropertiesManagerException {
        if (FileHelper.isInvalidDirectory(dir)) {
            throw new PropertiesManagerException("Directorio inválido o no legible: " + dir);
        }
    }

    private void validateProperties(Properties props) throws PropertiesManagerException {
        if (PropertiesHelper.isPropertiesInvalid(props)) {
            throw new PropertiesManagerException("Properties inválido: " + props);
        }
    }
}

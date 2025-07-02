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

/**
 * Implementación de la clase
 */
public class PropertiesManagerServiceImpl implements PropertiesManagerService {

    /** LOGGER asociado al componente */
    private static final Logger LOGGER = LogManager.getLogger(PropertiesManagerServiceImpl.class);

    /** Instanciación de la clase */
    private static final PropertiesManagerServiceImpl INSTANCE = new PropertiesManagerServiceImpl();

    /**  Objecto para la gestión de los properties como JSON */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /** Variable que contiene el directorio por defecto */
    private volatile String configDir = Constantes.DEFAULT_CONFIG_DIR;

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

        //
        File dir = new File(getConfigDir());
        LOGGER.debug("[loadAllProperties] - Obtenido el directorio mediante getConfigDir: {}.", dir);

        //
        validateDirectory(dir);
        LOGGER.debug("[loadAllProperties] - Obtenido el directorio mediante getConfigDir: {}.", dir);

        Map<String, Properties> temp = new HashMap<>();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(Constantes.PROPERTIES_EXT));

        //
        if (files == null || files.length == 0) {
            LOGGER.warn("[loadAllProperties] - No se encontraron .properties en {}", dir.getAbsolutePath());
            this.propertiesMap = Collections.emptyMap();
            return;
        }

        //
        LOGGER.debug("[loadAllProperties] - Array de Ficheros aosciados al directorio: {}.", Arrays.toString(files));

        for (File f : files) {
            Properties p = loadPropertiesFromFile(f);
            String key = stripExtension(f.getName());
            temp.put(key, p);
            LOGGER.debug("[loadAllProperties] - Archivo cargado: {} ({} propiedades)", f.getName(), p.size());
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

        validateFileName(fileName);
        LOGGER.debug("[addProperties] - Validación correcta del Filename: {}", fileName);

        validateProperties(props);
        LOGGER.debug("[addProperties] - Validación correcta del Properties: {}", props);

        Map<String, Properties> newMap = new HashMap<>(propertiesMap);
        LOGGER.debug("[addProperties] - Creación de un nuevo Map<String, Properties>");
        Properties prev = newMap.put(fileName, props);
        LOGGER.debug("[addProperties] - Creación de un nuevo Properties a partir de newMap.put(fileName, props)");
        propertiesMap = Collections.unmodifiableMap(newMap);
        LOGGER.debug("[addProperties] - Establezco que propertiesMap sea Collections.unmodifiableMap(newMap)");
        LOGGER.debug(prev == null
                        ? "[addProperties] - Añadido '{}', {} propiedades"
                        : "[addProperties] - Reemplazado '{}', antes {} propiedades, ahora {}",
                fileName, props.size(), prev == null ? 0 : prev.size());
    }

    @Override
    public void printProperties(String fileName) throws PropertiesManagerException {

        //
        validateFileName(fileName);
        LOGGER.debug("[printProperties] - Validación correcta del fileName: {}", fileName);
        validateMap(propertiesMap);
        LOGGER.debug("[printProperties] - Validación correcta del Map<String, Properties>: {}", propertiesMap);
        Properties props = propertiesMap.get(fileName);
        LOGGER.debug("[printProperties] - Obtengo las propiedades propertiesMap.get(fileName) {}", props);
        validateProperties(props);
        LOGGER.debug("[printProperties] - Validación correcta del Properties: {}", props);
        printPropertiesInternal(props);
    }

    @Override
    public void printAllProperties() throws PropertiesManagerException {

        //
        validateMap(propertiesMap);
        LOGGER.debug("[printAllProperties] - Validación correcta del Map<String, Properties>: {}", propertiesMap);
        propertiesMap.forEach((k, props) -> {
            LOGGER.info("[printAllProperties] === {}{} ===", k, Constantes.PROPERTIES_EXT);
            printPropertiesInternal(props);
        });
    }

    @Override
    public Properties getProperties(String fileName) throws PropertiesManagerException {

        //
        validateFileName(fileName);
        LOGGER.debug("[getProperties] - Validación correcta del fileName: {}", fileName);
        validateMap(propertiesMap);
        LOGGER.debug("[getProperties] - Validación correcta del Map<String, Properties>: {}", propertiesMap);

        return Optional.ofNullable(propertiesMap.get(fileName))
                .map(props -> {
                    LOGGER.debug("[getProperties] - {} propiedades recuperadas", props.size());
                    return props;
                })
                .orElseGet(Properties::new);
    }

    @Override
    public String getProperty(String fileName, String key) throws PropertiesManagerException {

        validateFileName(fileName);
        LOGGER.debug("[getProperty] - Validación correcta de fileName: {}", fileName);
        validateKey(key);
        LOGGER.debug("[getProperty] - Validación correcta de la Key: {}", key);
        validateMap(propertiesMap);
        LOGGER.debug("[getProperty] - Validación correcta de Map: {}", propertiesMap);
        Properties props = propertiesMap.get(fileName);
        LOGGER.debug("[getProperty] - Creación de objeto Properties a partir de filename");
        validateProperties(props);
        LOGGER.debug("[getProperty] - Validación correcta de Properties: {}", props);

        if (props.containsKey(key)) {
            String value =  props.getProperty(key);
            LOGGER.debug("[getProperty] - La key figura en el filename. Filename: {}; Key: {}; Value: {}", fileName, key, value);
            return value;
        }

        LOGGER.debug("[getProperty] - La key NO figura en el filename. Filename {}; Key: {}", fileName, key);
        // En caso de no encontrar la key devuelvo null
        return null;
    }

    @Override
    public List<String> getListFiles() throws PropertiesManagerException {
        validateMap(propertiesMap);
        LOGGER.debug("[getListFiles] - Validación correcta de Map: {}", propertiesMap);
        List<String> listFiles = new ArrayList<>(propertiesMap.keySet());
        LOGGER.debug("[getListFiles] - Lista de ficheros: {}", listFiles);
        return listFiles;
    }

    @Override
    public Map<String, Properties> getAllProperties() throws PropertiesManagerException {
        validateMap(propertiesMap);
        LOGGER.debug("[getAllProperties] - Validación correcta de Map: {}", propertiesMap);
        return propertiesMap;
    }

    @Override
    public boolean validateRequiredKeys(String fileName, Set<String> requiredKeys) throws PropertiesManagerException {

        validateFileName(fileName);
        LOGGER.debug("[validateRequiredKeys] - Validación correcta de fileName: {}", fileName);
        validateMap(propertiesMap);
        LOGGER.debug("[validateRequiredKeys] - Validación correcta de Map: {}", propertiesMap);

        if (requiredKeys == null || requiredKeys.isEmpty()) {
            LOGGER.debug("[validateRequiredKeys] - El conjunto de requiredKeys es NULL | Emptye. Devuelvo true.");
            return true;
        }

        Properties props = propertiesMap.get(fileName);
        LOGGER.debug("[validateRequiredKeys] - Obtengo el objeto Properties a partir del fichero: {}", fileName);
        if (props == null) {
            LOGGER.debug("[validateRequiredKeys] - El objeto Properties es Null. Devuelvo false.");
            return false;
        }

        Set<String> missing = requiredKeys.stream()
                .filter(k -> !props.containsKey(k))
                .collect(Collectors.toSet());
        LOGGER.debug("[validateRequiredKeys] - Conjunto missing: {}", missing);

        boolean empty = missing.isEmpty();
        LOGGER.debug("[validateRequiredKeys] - Conjunto missing es empty: {}", empty);

        return empty;
    }

    @Override
    public String exportPropertiesToJson(String fileName, boolean maskSensitive) throws PropertiesManagerException {

        validateFileName(fileName);
        LOGGER.debug("[exportPropertiesToJson] - Validación correcta de fileName: {}", fileName);
        validateMap(propertiesMap);
        LOGGER.debug("[exportPropertiesToJson] - Validación correcta de Map: {}", propertiesMap);

        Properties props = propertiesMap.get(fileName);
        LOGGER.debug("[exportPropertiesToJson] - Obtengo el objeto Properties a partir del fichero: {}", fileName);
        if (props == null) {
            String msg = "[exportPropertiesToJson] - Obtengo el objeto Properties es NULL";
            LOGGER.debug(msg);
            throw new PropertiesManagerException(msg);
        }

        Map<String, String> map = props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> maskSensitive && isSensitiveKey(e.getKey().toString())
                                ? Constantes.KEY_SENSITIVE_VALUE
                                : e.getValue().toString()
                ));
        LOGGER.debug("[exportPropertiesToJson] - Map<String, String> generado con Key_Sensitivo: {}", map);

        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            String msg = String.format("[exportPropertiesToJson] - Error exporando a JSON el Map: %s. Error: %s", map, ex.getMessage());
            LOGGER.error(msg, ex);
            throw new PropertiesManagerException(msg, ex);
        }
    }

    @Override
    public String exportAllPropertiesToJson(boolean maskSensitive) throws PropertiesManagerException {

        validateMap(propertiesMap);
        LOGGER.debug("[exportAllPropertiesToJson] - Validación correcta de Map: {}", propertiesMap);

        Map<String, Map<String, String>> all = new HashMap<>();
        LOGGER.debug("[exportAllPropertiesToJson] - Creación de un nuevo objeto Map<String, Map<String, String>>");

        propertiesMap.forEach((k, props) -> all.put(k, props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> maskSensitive && isSensitiveKey(e.getKey().toString())
                                ? Constantes.KEY_SENSITIVE_VALUE
                                : e.getValue().toString()
                ))
        ));

        LOGGER.debug("[exportAllPropertiesToJson] - Datos del objeto Map<String, Map<String, String>>: {}", all);

        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(all);
        } catch (JsonProcessingException ex) {
            String msg = String.format("[exportPropertiesToJson] - Error exporando a JSON el Map: %s. Error: %s", all, ex.getMessage());
            LOGGER.error(msg, ex);
            throw new PropertiesManagerException(msg, ex);
        }
    }

    // ============================================================
    //                  <<< Private Helpers >>>
    // ============================================================

    private Properties loadPropertiesFromFile(File file) throws PropertiesManagerException {

        validateFile(file);
        LOGGER.debug("[loadPropertiesFromFile] - Ficher correcto: {}.", file.getAbsoluteFile());

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            Properties props = new Properties();
            LOGGER.debug("[loadPropertiesFromFile] - Creación de un nuevo objeto Properties.");
            props.load(in);
            LOGGER.debug("[loadPropertiesFromFile] - Cargamdo las propiedades desde el fichero: {}.", file.getAbsoluteFile());
            return props;
        } catch (IOException ex) {
            String msg = String.format("Error en el archivo %s: %s", file.getAbsolutePath(), ex.getMessage());
            LOGGER.error(msg, ex);
            throw new PropertiesManagerException("Error cargando archivo " + file.getName(), ex);
        }
    }

    private void printPropertiesInternal(Properties props) throws PropertiesManagerException {

        validateProperties(props);
        LOGGER.debug("[printPropertiesInternal] - Validación correcta de Properties: {}", props);

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

        if (StringHelper.isStringInvalid(key)) {
            LOGGER.debug("[isSensitiveKey] - isSensitiveKey({}): ", key);
            return false;
        }

        LOGGER.debug("[isSensitiveKey] - : {}", key);
        String lower = key.toLowerCase(Locale.ROOT);
        LOGGER.debug("[isSensitiveKey] - Pasando key a minusculas. key: {}, lower: {}", key, lower);
        boolean keySensitive = sensitiveKeys.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(lower::contains);
        LOGGER.debug("[isSensitiveKey] - isSensitiveKey({}): {}", key, keySensitive);
        return keySensitive;

    }

    private String stripExtension(String name) throws PropertiesManagerException {

        //
        validateFileName(name);
        LOGGER.debug("[stripExtension] - Validación correcta de filename: {}", name);
        int idx = name.lastIndexOf('.');
        LOGGER.debug("[stripExtension] - Índice del '.': {}", idx);
        String filenameWithoutExtension = (idx == -1) ? name : name.substring(0, idx);
        LOGGER.debug("[stripExtension] - Filename sin extensión: {}", filenameWithoutExtension);
        return filenameWithoutExtension;
    }

    private <K,V> void validateMap(Map<K,V> map) throws PropertiesManagerException {
        if (MapHelper.isMapInvalid(map)) {
            String msg = String.format("Map no válido. Map: %s", map);
            LOGGER.error(msg);
            throw new PropertiesManagerException(msg);
        }
    }

    private void validateFileName(String name) throws PropertiesManagerException {
        if (StringHelper.isStringInvalid(name)) {
            String msg = String.format("Nombre de archivo inválido. String: %s", name);
            LOGGER.error(msg);
            throw new PropertiesManagerException(msg);
        }
    }

    private void validateKey(String key) throws PropertiesManagerException {
        if (StringHelper.isStringInvalid(key)) {
            String msg = String.format("Clave inválida. String: %s", key);
            LOGGER.error(msg);
            throw new PropertiesManagerException(msg);
        }
    }

    private void validateFile(File file) throws PropertiesManagerException {
        if (FileHelper.isInvalidFile(file)) {
            String msg = String.format("Archivo inválido o no legible. Properties: %s", file);
            LOGGER.error(msg);
            throw new PropertiesManagerException(msg);
        }
    }

    private void validateDirectory(File dir) throws PropertiesManagerException {
        if (FileHelper.isInvalidDirectory(dir)) {
            String msg = String.format("Directorio inválido o no legible. File: %s", dir);
            LOGGER.error(msg);
            throw new PropertiesManagerException(msg);
        }
    }

    private void validateProperties(Properties props) throws PropertiesManagerException {
        if (PropertiesHelper.isPropertiesInvalid(props)) {
            String msg = String.format("Properties inválido o no legible. Properties: %s", props);
            LOGGER.error(msg);
            throw new PropertiesManagerException(msg);
        }
    }
}

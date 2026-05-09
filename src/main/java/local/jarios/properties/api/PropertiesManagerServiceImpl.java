package local.jarios.properties.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.jarios.properties.common.util.Constantes;
import local.jarios.properties.exception.PropertiesManagerException;
import local.jarios.properties.helpers.FileHelper;
import local.jarios.properties.helpers.MapHelper;
import local.jarios.properties.helpers.PropertiesHelper;
import local.jarios.properties.helpers.StringHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación singleton del servicio {@link PropertiesManagerService}.
 * <p>
 * Permite cargar, obtener, modificar y exportar archivos de propiedades desde un
 * directorio configurable, con soporte de claves sensibles y exportación a JSON.
 * </p>
 *
 * @author Juan Antonio
 * @version 2.1
 * @since 2024-06-18
 */
@Slf4j
public class PropertiesManagerServiceImpl implements PropertiesManagerService {

  private static final PropertiesManagerServiceImpl INSTANCE = new PropertiesManagerServiceImpl();
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private volatile String configDir = Constantes.PROPERTIES_DIR;
  private volatile Map<String, Properties> propertiesMap = Collections.emptyMap();
  private volatile Set<String> sensitiveKeys = Constantes.DEFAULT_SENSITIVE_KEYS;

  private PropertiesManagerServiceImpl() {}

  public static PropertiesManagerService getInstance() {
    return INSTANCE;
  }

  @Override
  public String getConfigDir() {
    return (configDir == null || configDir.isBlank()) ? Constantes.PROPERTIES_DIR : configDir;
  }

  @Override
  public void setConfigDir(String configDir) {
    this.configDir = (configDir == null || configDir.isBlank()) ? Constantes.PROPERTIES_DIR : configDir.trim();
    log.debug("Directorio de configuración actualizado a '{}'", this.configDir);
  }

  @Override
  public synchronized Set<String> getSensitiveKeys() {
    return Set.copyOf(sensitiveKeys);
  }

  @Override
  public synchronized void setSensitiveKeys(Set<String> keys) {
    this.sensitiveKeys = (keys == null || keys.isEmpty())
        ? Constantes.DEFAULT_SENSITIVE_KEYS
        : Set.copyOf(keys);
    log.debug("Claves sensibles definidas: {}", this.sensitiveKeys);
  }

  @Override
  public synchronized void loadAllProperties() throws PropertiesManagerException {
    File dir = new File(getConfigDir());
    validateDirectory(dir);

    File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".properties"));
    if (files == null || files.length == 0) {
      log.warn("No se encontraron archivos .properties en '{}'", dir.getAbsolutePath());
      propertiesMap = Collections.emptyMap();
      return;
    }

    Map<String, Properties> tempMap = new HashMap<>();
    for (File f : files) {
      tempMap.put(normalizeFileName(f.getName()), loadPropertiesFromFile(f));
    }

    propertiesMap = Collections.unmodifiableMap(tempMap);
    log.debug("Se cargaron {} archivos de propiedades", propertiesMap.size());
  }

  @Override
  public synchronized void reload() throws PropertiesManagerException {
    loadAllProperties();
  }

  @Override
  public synchronized void addProperties(String fileName, Properties props)
      throws PropertiesManagerException {
    validateFileName(fileName);
    validateProperties(props);

    String normalizedFileName = normalizeFileName(fileName);
    Map<String, Properties> newMap = new HashMap<>(propertiesMap);
    Properties previous = newMap.put(normalizedFileName, copyProperties(props));
    propertiesMap = Collections.unmodifiableMap(newMap);

    log.debug(previous == null
                 ? "Archivo '{}' añadido con {} propiedades"
                 : "Archivo '{}' reemplazado: antes {} propiedades, ahora {}",
             normalizedFileName, props.size(), previous == null ? 0 : previous.size(), props.size());
  }

  @Override
  public boolean hasLoaded(String fileName) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);
    return propertiesMap.containsKey(normalizeFileName(fileName));
  }

  @Override
  public synchronized void setProperty(String fileName, String property, String value)
      throws PropertiesManagerException {
    validateFileName(fileName);
    validateKey(property);

    String normalizedFileName = normalizeFileName(fileName);
    Map<String, Properties> newMap = new HashMap<>(propertiesMap);
    Properties props = copyProperties(newMap.getOrDefault(normalizedFileName, new Properties()));
    props.setProperty(property, value);
    newMap.put(normalizedFileName, props);
    propertiesMap = Collections.unmodifiableMap(newMap);

    log.debug("Propiedad '{}' de '{}' actualizada a '{}'",
        property, normalizedFileName, maskIfSensitive(property, value));
  }

  @Override
  public void printProperties(String fileName) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);

    Properties props = propertiesMap.get(normalizeFileName(fileName));
    if (props == null) {
      throw new PropertiesManagerException("Archivo no cargado: " + fileName);
    }

    printPropertiesInternal(props);
  }

  @Override
  public void printAllProperties() throws PropertiesManagerException {
    validateMap(propertiesMap);
    propertiesMap.forEach((k, props) -> {
      log.debug("Propiedades de '{}':", k);
      printPropertiesInternal(props);
    });
  }

  @Override
  public Properties getProperties(String fileName) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);
    return copyProperties(propertiesMap.getOrDefault(normalizeFileName(fileName), new Properties()));
  }

  @Override
  public synchronized String getProperty(String fileName, String key) throws PropertiesManagerException {
    validateFileName(fileName);
    validateKey(key);

    Properties props = propertiesMap.get(normalizeFileName(fileName));
    String value = props == null ? null : props.getProperty(key);
    if (value != null) {
      return value;
    }

    value = System.getenv(key);
    if (value != null) {
      return value;
    }

    value = System.getProperty(key);
    if (value != null) {
      return value;
    }

    throw new PropertiesManagerException(
        String.format("Clave inexistente '%s' en '%s'", key, fileName));
  }

  @Override
  public List<String> getListFiles() throws PropertiesManagerException {
    validateMap(propertiesMap);
    return new ArrayList<>(propertiesMap.keySet());
  }

  @Override
  public Map<String, Properties> getAllProperties() throws PropertiesManagerException {
    Map<String, Properties> copy = new HashMap<>();
    propertiesMap.forEach((file, props) -> copy.put(file, copyProperties(props)));
    return Collections.unmodifiableMap(copy);
  }

  @Override
  public boolean validateRequiredKeys(String fileName, Set<String> requiredKeys) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);

    if (requiredKeys == null || requiredKeys.isEmpty()) return true;

    Properties props = propertiesMap.get(normalizeFileName(fileName));
    if (props == null) return false;

    return requiredKeys.stream().allMatch(props::containsKey);
  }

  @Override
  public String exportPropertiesToJson(String fileName, boolean maskSensitive) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);

    Properties props = propertiesMap.get(normalizeFileName(fileName));
    if (props == null) {
      throw new PropertiesManagerException("Archivo no cargado: " + fileName);
    }

    return writeJson(maskSensitiveKeys(props, maskSensitive));
  }

  @Override
  public String exportAllPropertiesToJson(boolean maskSensitive) throws PropertiesManagerException {
    validateMap(propertiesMap);

    Map<String, Map<String, String>> allMap = new HashMap<>();
    propertiesMap.forEach((file, props) -> allMap.put(file, maskSensitiveKeys(props, maskSensitive)));

    return writeJson(allMap);
  }

  // ------------------- MÉTODOS PRIVADOS -------------------

  private Properties loadPropertiesFromFile(File file) throws PropertiesManagerException {
    validateFile(file);

    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
      Properties props = new Properties();
      props.load(in);
      log.debug("Archivo '{}' cargado con {} propiedades", file.getName(), props.size());
      return copyProperties(props);
    } catch (IOException ex) {
      log.error("Error cargando archivo '{}'", file.getName(), ex);
      throw new PropertiesManagerException("Error cargando archivo " + file.getName(), ex);
    }
  }

  private void printPropertiesInternal(Properties props) {
    props.forEach((k, v) -> log.debug("{} = {}", k, maskIfSensitive(k.toString(), v.toString())));
  }

  private String maskIfSensitive(String key, String value) {
    return isSensitiveKey(key) ? Constantes.KEY_SENSITIVE_VALUE : value;
  }

  private boolean isSensitiveKey(String key) {
    if (StringHelper.isStringInvalid(key)) return false;
    String lower = key.toLowerCase(Locale.ROOT);
    return sensitiveKeys.stream().map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(lower::contains);
  }

  private String normalizeFileName(String filename) throws PropertiesManagerException {
    validateFileName(filename);
    String trimmed = filename.trim();
    return trimmed.toLowerCase(Locale.ROOT).endsWith(Constantes.PROPERTIES_EXT)
        ? trimmed.substring(0, trimmed.length() - Constantes.PROPERTIES_EXT.length())
        : trimmed;
  }

  private <K, V> void validateMap(Map<K, V> map) throws PropertiesManagerException {
    if (MapHelper.isMapInvalid(map)) {
      throw new PropertiesManagerException("Mapa de propiedades inválido");
    }
  }

  private void validateFileName(String filename) throws PropertiesManagerException {
    if (StringHelper.isStringInvalid(filename)) {
      throw new PropertiesManagerException("Nombre de archivo inválido");
    }
  }

  private void validateKey(String key) throws PropertiesManagerException {
    if (StringHelper.isStringInvalid(key)) {
      throw new PropertiesManagerException("Clave inválida");
    }
  }

  private void validateFile(File file) throws PropertiesManagerException {
    if (FileHelper.isInvalidFile(file)) {
      throw new PropertiesManagerException("Archivo inválido");
    }
  }

  private void validateDirectory(File dir) throws PropertiesManagerException {
    if (FileHelper.isInvalidDirectory(dir)) {
      throw new PropertiesManagerException("Directorio inválido");
    }
  }

  private void validateProperties(Properties props) throws PropertiesManagerException {
    if (PropertiesHelper.isPropertiesInvalid(props)) {
      throw new PropertiesManagerException("Properties inválido");
    }
  }

  private Map<String, String> maskSensitiveKeys(Properties props, boolean maskSensitive) {
    return props.entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey().toString(),
            e -> maskSensitive && isSensitiveKey(e.getKey().toString())
                ? Constantes.KEY_SENSITIVE_VALUE
                : e.getValue().toString()
        ));
  }

  private Properties copyProperties(Properties source) {
    Properties copy = new Properties();
    if (source != null) {
      copy.putAll(source);
    }
    return copy;
  }

  private String writeJson(Object obj) throws PropertiesManagerException {
    try {
      return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException ex) {
      throw new PropertiesManagerException("Error exportando a JSON", ex);
    }
  }
}

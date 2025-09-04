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
 *
 * <p>
 * Este servicio permite cargar, obtener, modificar y exportar archivos de propiedades desde un
 * directorio configurable.</p>
 *
 * @author Juan Antonio
 * @version 2.0
 * @since 2024-06-18
 */
public class PropertiesManagerServiceImpl implements PropertiesManagerService {

  /**
   * LOGGER.
   */
  private static final Logger LOGGER = LogManager.getLogger(PropertiesManagerServiceImpl.class);

  /**
   * Instancia del objeto.
   */
  private static final PropertiesManagerServiceImpl INSTANCE = new PropertiesManagerServiceImpl();

  /**
   * Para la exportación a JSON.
   */
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  /**
   * Directorio de configuración donde se encuentran los ficheros properties.
   */
  private volatile String configDir = Constantes.PROPERTIES_DIR;

  /**
   * Mapa que contendrá todas las propiedades de todos los ficheros.
   */
  private volatile Map<String, Properties> propertiesMap = Collections.emptyMap();

  /**
   * Conjunto de Key Sensitive.
   */
  private volatile Set<String> sensitiveKeys = Collections.emptySet();

  /**
   * Constructor privado para evitar instanciaciones de la clase.
   */
  private PropertiesManagerServiceImpl() {
  }

  /**
   * Devuelve la instancia singleton del servicio de propiedades.
   *
   * @return instancia única del servicio
   */
  public static PropertiesManagerService getInstance() {

    return INSTANCE;
  }

  /**
   * Devuelve el directorio actual de configuración.
   *
   * @return ruta al directorio configurado
   */
  @Override
  public String getConfigDir() {
    return (configDir == null || configDir.isBlank()) ? Constantes.PROPERTIES_DIR : configDir;
  }

  /**
   * Establece el directorio desde donde se cargarán los archivos de propiedades.
   *
   * @param configDir directorio con los archivos .properties; si es nulo o vacío se usa el valor
   *                  por defecto
   * @throws PropertiesManagerException si ocurre un error de validación
   */
  @Override
  public void setConfigDir(String configDir) throws PropertiesManagerException {
    this.configDir = (configDir == null || configDir.isBlank())
        ? Constantes.PROPERTIES_DIR
        : configDir.trim();
    LOGGER.debug("[setConfigDir] - Nueva configuración: {}", this.configDir);
  }

  /**
   * Obtiene el conjunto actual de claves sensibles.
   *
   * @return conjunto de claves sensibles
   */
  @Override
  public Set<String> getSensitiveKeys() {
    return sensitiveKeys;
  }

  /**
   * Define las claves sensibles que serán ocultadas al exportar o imprimir.
   *
   * @param keys conjunto de claves sensibles
   */
  @Override
  public synchronized void setSensitiveKeys(Set<String> keys) {
    this.sensitiveKeys = (keys == null) ? Collections.emptySet() : Set.copyOf(keys);
    LOGGER.debug(
        "[setSensitiveKeys] - Claves sensibles actualizadas: {}",
        this.sensitiveKeys);
  }

  /**
   * Carga todos los archivos .properties del directorio configurado.
   *
   * @throws PropertiesManagerException si ocurre algún error en el proceso
   */
  @Override
  public synchronized void loadAllProperties() throws PropertiesManagerException {
    File dir = new File(getConfigDir());
    validateDirectory(dir);

    File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".properties"));
    if (files == null || files.length == 0) {
      LOGGER.warn("No se encontraron .properties en {}", dir.getAbsolutePath());
      propertiesMap = Collections.emptyMap(); // Borrar todo
      return;
    }

    Map<String, Properties> tempMap = new HashMap<>();
    for (File f : files) {
      Properties props = loadPropertiesFromFile(f);
      String key = stripExtension(f.getName());
      tempMap.put(key, props);
    }

    propertiesMap = Collections.unmodifiableMap(tempMap); // Nuevo mapa inmutable
  }

  /**
   * Recarga todos los archivos de propiedades.
   *
   * @throws PropertiesManagerException si ocurre un error
   */
  @Override
  public synchronized void reload() throws PropertiesManagerException {
    loadAllProperties();
  }

  /**
   * Agrega o reemplaza un conjunto de propiedades en memoria.
   *
   * @param fileName nombre del archivo lógico.
   * @param props    propiedades a almacenar.
   * @throws PropertiesManagerException si hay errores de validación.
   */
  @Override
  public void addProperties(String fileName, Properties props) throws PropertiesManagerException {
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

  /**
   * Devievle un booleano indicando si un fichero de propiedades ya ha sido cargado.
   *
   * @param fileName fichero que analizamos.
   * @return boolean con el valor indicando si se ha cargado o no.
   */
  @Override
  public boolean hasLoaded(String fileName) throws PropertiesManagerException {
    validateMap(propertiesMap);
    return propertiesMap.containsKey(fileName);
  }

  /**
   * Agrega o reemplaza una propiedad dentro de un fichero.
   *
   * @param fileName nombre del archivo lógico
   * @param property propiedad donde se almacenará
   * @param valor    que se almacenará
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public void setProperty(String fileName, String property, String valor)
      throws PropertiesManagerException {
    validateFileName(fileName);
    validateKey(property);

    Map<String, Properties> newMap = new HashMap<>(propertiesMap); // copia mutable
    Properties props = newMap.get(fileName);
    if (props == null) {
      props = new Properties();
      newMap.put(fileName, props);
    }
    props.setProperty(property, valor);

    propertiesMap = Collections.unmodifiableMap(newMap); // volver a inmutable

    LOGGER.debug("[setProperty] - '{}' actualizado: {}={}", fileName, property, valor);
  }

  /**
   * Imprime por consola las propiedades asociadas a un archivo.
   *
   * @param fileName nombre del archivo lógico
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public void printProperties(String fileName) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);
    Properties props = propertiesMap.get(fileName);
    if (props == null) {
      throw new PropertiesManagerException("Archivo no cargado: " + fileName);
    }
    printPropertiesInternal(props);
  }

  /**
   * Imprime todas las propiedades de todos los archivos cargados.
   *
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public void printAllProperties() throws PropertiesManagerException {
    if (propertiesMap == null) {
      throw new PropertiesManagerException("Mapa no cargado.");
    }
    propertiesMap.forEach((k, props) -> {
      LOGGER.debug("[printAllProperties] === {}{} ===", k, Constantes.PROPERTIES_EXT);
      printPropertiesInternal(props);
    });
  }

  /**
   * Devuelve las propiedades asociadas a un archivo.
   *
   * @param fileName nombre del archivo
   * @return objeto {@link Properties}
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public Properties getProperties(String fileName) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);
    return propertiesMap.getOrDefault(fileName, new Properties());
  }

  /**
   * Devuelve el valor asociado a una clave en un archivo.
   *
   * @param fileName nombre del archivo
   * @param key      clave a buscar
   * @return valor o {@code null} si no se encuentra
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public String getProperty(String fileName, String key) throws PropertiesManagerException {
    validateFileName(fileName);
    validateKey(key);
    Properties props = getProperties(fileName); // ya devuelve Properties vacío si no existe
    String value = props.getProperty(key);
    if (value == null) {
      throw new PropertiesManagerException(
          "Clave inexistente: " + key + " en " + fileName);
    }
    return value;
  }

  /**
   * Devuelve la lista de archivos cargados.
   *
   * @return lista de nombres de archivo
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public List<String> getListFiles() throws PropertiesManagerException {
    validateMap(propertiesMap);
    return new ArrayList<>(propertiesMap.keySet());
  }

  /**
   * Devuelve el mapa completo de archivos y sus propiedades.
   *
   * @return mapa inmutable de propiedades
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public Map<String, Properties> getAllProperties() throws PropertiesManagerException {
    if (propertiesMap == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(propertiesMap);
  }

  /**
   * Verifica si todas las claves requeridas existen en el archivo especificado.
   *
   * @param fileName     nombre del archivo
   * @param requiredKeys claves requeridas
   * @return {@code true} si todas existen, {@code false} si falta alguna
   * @throws PropertiesManagerException si hay errores de validación
   */
  @Override
  public boolean validateRequiredKeys(String fileName, Set<String> requiredKeys) throws PropertiesManagerException {
    validateFileName(fileName);
    validateMap(propertiesMap);

    if (requiredKeys == null || requiredKeys.isEmpty()) return true;

    Properties props = propertiesMap.get(fileName);
    if (props == null) return false;

    return requiredKeys.stream().allMatch(props::containsKey);
  }

  /**
   * Exporta las propiedades de un archivo a formato JSON.
   *
   * @param fileName      nombre del archivo
   * @param maskSensitive si {@code true}, oculta claves sensibles
   * @return representación JSON
   * @throws PropertiesManagerException si hay errores de validación o serialización
   */
  @Override
  public String exportPropertiesToJson(String fileName, boolean maskSensitive)
      throws PropertiesManagerException {
    validateFileName(fileName);
    if (propertiesMap == null) {
      throw new PropertiesManagerException(
          "[exportPropertiesToJson] - El mapa no se encuentra cargado.");
    }
    Properties props = propertiesMap.get(fileName);
    if (props == null) {
      throw new PropertiesManagerException(
          "[exportPropertiesToJson] - Archivo no cargado: " + fileName + ".");
    }

    Map<String, String> filteredMap = maskSensitiveKeys(props, maskSensitive);

    return writeJson(filteredMap);
  }

  /**
   * Exporta todas las propiedades de todos los archivos a JSON.
   *
   * @param maskSensitive si {@code true}, oculta claves sensibles
   * @return JSON con todas las propiedades
   * @throws PropertiesManagerException si ocurre un error
   */
  @Override
  public String exportAllPropertiesToJson(boolean maskSensitive) throws PropertiesManagerException {
    validateMap(propertiesMap);

    Map<String, Map<String, String>> allMap = new HashMap<>();
    for (Map.Entry<String, Properties> entry : propertiesMap.entrySet()) {
      allMap.put(entry.getKey(), maskSensitiveKeys(entry.getValue(), maskSensitive));
    }

    return writeJson(allMap);
  }

  // ------------------- MÉTODOS PRIVADOS -------------------

  /**
   * Cargar propiedades desde fichero.
   *
   * @param file fichero desde el que se cargan las propiedades
   * @return properties objeto con las propiedades cargadas desde un fichero
   * @throws PropertiesManagerException excepción
   */
  private Properties loadPropertiesFromFile(File file) throws PropertiesManagerException {
    LOGGER.debug(file.getName());
    validateFile(file);

    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
      Properties props = new Properties();
      props.load(in);
      LOGGER.debug(props);
      return props;
    } catch (IOException ex) {
      throw new PropertiesManagerException("Error cargando archivo " + file.getName(), ex);
    }
  }

  /**
   * Imprime prodiedades.
   *
   * @param props properties a imprimir
   * @throws PropertiesManagerException excepción
   */
  private void printPropertiesInternal(Properties props) throws PropertiesManagerException {
    props.forEach((k, v) -> {
      String val = isSensitiveKey(k.toString()) ? Constantes.KEY_SENSITIVE_VALUE : v.toString();
      LOGGER.debug("{} = {}", k, val);
    });
  }

  /**
   * Verificar si es una Sensitive Key.
   *
   * @param key fcihero con extensión
   * @return boolean
   * @throws PropertiesManagerException excepción
   */
  private boolean isSensitiveKey(String key) {
    if (StringHelper.isStringInvalid(key)) {
      return false;
    }
    String lower = key.toLowerCase(Locale.ROOT);
    return sensitiveKeys.stream()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .anyMatch(lower::contains);
  }

  /**
   * Eliminar extensión.
   *
   * @param filename fcihero con extensión
   * @return string
   * @throws PropertiesManagerException excepción
   */
  private String stripExtension(String filename) throws PropertiesManagerException {
    validateFileName(filename);
    int idx = filename.lastIndexOf('.');
    return (idx == -1) ? filename : filename.substring(0, idx);
  }

  /**
   * Valida que un mapa no sea nulo ni vacío.
   *
   * @param <K> tipo de clave del mapa
   * @param <V> tipo de valor del mapa
   * @param map el mapa a validar
   * @throws PropertiesManagerException si el mapa es inválido
   */
  private <K, V> void validateMap(Map<K, V> map) throws PropertiesManagerException {
    if (MapHelper.isMapInvalid(map)) {
      throw new PropertiesManagerException("Map inválido");
    }
  }

  /**
   * Validar key.
   *
   * @param filename string
   * @throws PropertiesManagerException excepción
   */
  private void validateFileName(String filename) throws PropertiesManagerException {
    if (StringHelper.isStringInvalid(filename)) {
      throw new PropertiesManagerException("Nombre de archivo inválido");
    }
  }

  /**
   * Validar key.
   *
   * @param key string
   * @throws PropertiesManagerException excepción
   */
  private void validateKey(String key) throws PropertiesManagerException {
    if (StringHelper.isStringInvalid(key)) {
      throw new PropertiesManagerException("Clave inválida");
    }
  }

  /**
   * Validar fichero.
   *
   * @param file fichero
   * @throws PropertiesManagerException excepción
   */
  private void validateFile(File file) throws PropertiesManagerException {
    if (FileHelper.isInvalidFile(file)) {
      throw new PropertiesManagerException("Archivo inválido");
    }
  }

  /**
   * Validar directorio.
   *
   * @param dir directorio
   * @throws PropertiesManagerException excepción
   */
  private void validateDirectory(File dir) throws PropertiesManagerException {
    if (FileHelper.isInvalidDirectory(dir)) {
      throw new PropertiesManagerException("Directorio inválido");
    }
  }

  /**
   * Validar properties.
   *
   * @param props properties
   * @throws PropertiesManagerException excepción
   */
  private void validateProperties(Properties props) throws PropertiesManagerException {
    if (PropertiesHelper.isPropertiesInvalid(props)) {
      throw new PropertiesManagerException("Properties inválido");
    }
  }

  /**
   * Método privado para enmascarar claves sensibles.
   *
   * @param props         Properties
   * @param maskSensitive Indica si se deben enmarcarar las claves sensibles
   * @return map devuelto
   */
  private Map<String, String> maskSensitiveKeys(Properties props, boolean maskSensitive) {
    return props.entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey().toString(),
            e -> maskSensitive && isSensitiveKey(e.getKey().toString())
                ? Constantes.KEY_SENSITIVE_VALUE
                : e.getValue().toString()
        ));
  }

  /**
   * Método privado para escribir JSON usando Jackson.
   *
   * @param obj Objeto que pasamos a json
   * @return String
   * @throws PropertiesManagerException excepción devuelta
   */
  private String writeJson(Object obj) throws PropertiesManagerException {
    try {
      return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException ex) {
      throw new PropertiesManagerException("Error exportando a JSON", ex);
    }
  }
}

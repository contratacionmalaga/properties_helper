package local.jarios.properties.api;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import local.jarios.properties.exception.PropertiesManagerException;

/**
 * Interfaz que define las operaciones para la gestión centralizada de ficheros .properties. Esta
 * API permite:
 * <ul>
 *   <li>Carga de múltiples ficheros .properties desde una carpeta externa.</li>
 *   <li>Acceso mediante copias defensivas para evitar modificaciones accidentales.</li>
 *   <li>Ocultamiento de claves sensibles en operaciones de impresión y exportación.</li>
 *   <li>Exportación de propiedades a formato JSON.</li>
 *   <li>Validación de la presencia de claves requeridas en un fichero.</li>
 *   <li>Recarga segura y sincronizada de todas las propiedades cargadas.</li>
 * </ul>
 *
 * @author Juan Antonio
 * @version 2.0
 * @since 2024-06-18
 */
public interface PropertiesManagerService {

  /**
   * Obtiene el conjunto de claves sensibles definidas.
   *
   * @return Conjunto de claves sensibles.
   */
  Set<String> getSensitiveKeys();

  /**
   * Establece las claves sensibles que deben ocultarse durante la impresión y exportación.
   *
   * @param keys Conjunto de claves sensibles. Si es {@code null} o vacío, se usarán las claves por
   *             defecto.
   * @throws PropertiesManagerException si ocurre un error al establecer las claves.
   */
  void setSensitiveKeys(Set<String> keys) throws PropertiesManagerException;

  /**
   * Obtiene el conjunto de claves sensibles definidas.
   *
   * @return Conjunto de claves sensibles.
   */
  List<String> getListFiles() throws PropertiesManagerException;

  /**
   * Carga todos los ficheros .properties desde el directorio de configuración. Reemplaza cualquier
   * carga previa.
   *
   * @throws PropertiesManagerException si falla la carga desde el directorio.
   */
  void loadAllProperties() throws PropertiesManagerException;

  /**
   * Imprime en el log las propiedades del fichero indicado, ocultando claves sensibles.
   *
   * @param fileNameWithoutExtension Nombre del fichero sin extensión (.properties).
   * @throws PropertiesManagerException si el nombre es inválido o el fichero no está cargado.
   */
  void printProperties(String fileNameWithoutExtension) throws PropertiesManagerException;

  /**
   * Imprime en el log todas las propiedades de todos los ficheros cargados. Se aplica
   * enmascaramiento a las claves sensibles.
   *
   * @throws PropertiesManagerException si ocurre un error durante la operación.
   */
  void printAllProperties() throws PropertiesManagerException;

  /**
   * Devuelve una copia defensiva de las propiedades de un fichero específico.
   *
   * @param fileNameWithoutExtension Nombre del fichero sin extensión.
   * @return Copia de las propiedades del fichero.
   * @throws PropertiesManagerException si el fichero no está disponible o el nombre es inválido.
   */
  Properties getProperties(String fileNameWithoutExtension) throws PropertiesManagerException;

  /**
   * Agrega o reemplaza una propiedad dentro de un fichero.
   *
   * @param fileName nombre del archivo lógico
   * @param property propiedad donde se almacenará
   * @param valor    que se almacenará
   * @throws PropertiesManagerException si hay errores de validación
   */
  void setProperty(String fileName, String property, String valor)
      throws PropertiesManagerException;

  /**
   * Devuelve un booleano indicando si un fichero de propiedades ya ha sido cargado.
   *
   * @param fileName fichero que analizamos.
   * @return boolean con el valor indicando si se ha cargado o no.
   */
  boolean hasLoaded(String fileName) throws PropertiesManagerException;

  /**
   * Devuelve el valor de una clave buscando en el fichero, variables de entorno o propiedades del
   * sistema.
   *
   * @param fileNameWithoutExtension Nombre del fichero sin extensión.
   * @param key                      Clave a buscar.
   * @return Valor encontrado.
   * @throws PropertiesManagerException si el nombre del fichero o la clave son inválidos, o si la
   *                                    clave no existe.
   */
  String getProperty(String fileNameWithoutExtension, String key) throws PropertiesManagerException;

  /**
   * Devuelve un mapa inmutable con copias defensivas de todos los ficheros de propiedades cargados.
   *
   * @return Mapa con el nombre del fichero como clave y sus {@code Properties} como valor.
   * @throws PropertiesManagerException si ocurre un error al acceder a los datos.
   */
  Map<String, Properties> getAllProperties() throws PropertiesManagerException;

  /**
   * Exporta las propiedades de un fichero específico en formato JSON.
   *
   * @param fileNameWithoutExtension Nombre del fichero sin extensión.
   * @param maskSensitiveValues      Si {@code true}, se ocultan valores sensibles.
   * @return Cadena JSON con las propiedades.
   * @throws PropertiesManagerException si ocurre un error al procesar o exportar las propiedades.
   */
  String exportPropertiesToJson(String fileNameWithoutExtension, boolean maskSensitiveValues)
      throws PropertiesManagerException;

  /**
   * Exporta todas las propiedades en formato JSON, agrupadas por fichero.
   *
   * @param maskSensitiveValues Si {@code true}, se ocultan valores sensibles.
   * @return Cadena JSON que representa todos los ficheros y sus propiedades.
   * @throws PropertiesManagerException si ocurre un error durante la conversión a JSON.
   */
  String exportAllPropertiesToJson(boolean maskSensitiveValues) throws PropertiesManagerException;

  /**
   * Verifica si un fichero contiene todas las claves requeridas.
   *
   * @param fileNameWithoutExtension Nombre del fichero sin extensión.
   * @param requiredKeys             Conjunto de claves que deben existir.
   * @return {@code true} si todas las claves están presentes; {@code false} en caso contrario.
   * @throws PropertiesManagerException si el fichero es inválido o no se puede acceder.
   */
  boolean validateRequiredKeys(String fileNameWithoutExtension, Set<String> requiredKeys)
      throws PropertiesManagerException;

  /**
   * Recarga todas las propiedades desde el directorio configurado.
   *
   * @throws PropertiesManagerException si falla la recarga.
   */
  void reload() throws PropertiesManagerException;

  /**
   * Obtiene el directorio actualmente configurado para la carga de ficheros.
   *
   * @return Ruta del directorio de configuración.
   * @throws PropertiesManagerException si ocurre un error al acceder a la configuración.
   */
  String getConfigDir() throws PropertiesManagerException;

  /**
   * Establece el directorio desde donde se cargarán los ficheros .properties. Si es {@code null} o
   * vacío, se usa la ruta por defecto.
   *
   * @param configDir Ruta del directorio.
   * @throws PropertiesManagerException si la ruta es inválida o no accesible.
   */
  void setConfigDir(String configDir) throws PropertiesManagerException;

  /**
   * Añade o reemplaza un conjunto de propiedades en memoria para un fichero específico.
   *
   * @param fileName   Nombre del fichero sin extensión.
   * @param properties Propiedades a almacenar.
   * @throws PropertiesManagerException si los parámetros son inválidos o ocurre un error de
   *                                    almacenamiento.
   */
  void addProperties(String fileName, Properties properties) throws PropertiesManagerException;
}

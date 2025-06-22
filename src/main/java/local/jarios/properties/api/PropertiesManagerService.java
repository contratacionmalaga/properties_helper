package local.jarios.properties.api;

import local.jarios.properties.exception.PropertiesManagerException;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Interfaz que define las operaciones para la gestión centralizada de ficheros .properties.
 * Esta API permite:
 * <ul>
 *   <li>Carga de múltiples ficheros .properties desde una carpeta externa o el classpath.</li>
 *   <li>Acceso inmutable a las propiedades para evitar modificaciones accidentales.</li>
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
     * Establece las claves sensibles que deben ocultarse durante la impresión y exportación.
     *
     * @param keys Conjunto de claves sensibles. Si es {@code null} o vacío, no se ocultará ninguna.
     * @throws PropertiesManagerException si ocurre un error al establecer las claves.
     */
    void setSensitiveKeys(Set<String> keys) throws PropertiesManagerException;

    /**
     * Obtiene el conjunto de claves sensibles definidas.
     *
     * @return Conjunto de claves sensibles.
     */
    Set<String> getSensitiveKeys();

    /**
     * Obtiene el conjunto de claves sensibles definidas.
     *
     * @return Conjunto de claves sensibles.
     */
    List<String> getListFiles();

    /**
     * Carga todos los ficheros .properties desde el directorio de configuración.
     * Reemplaza cualquier carga previa.
     *
     * @throws PropertiesManagerException si falla la carga desde el directorio o el classpath.
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
     * Imprime en el log todas las propiedades de todos los ficheros cargados.
     * Se aplica enmascaramiento a las claves sensibles.
     *
     * @throws PropertiesManagerException si ocurre un error durante la operación.
     */
    void printAllProperties() throws PropertiesManagerException;

    /**
     * Devuelve una copia inmutable de las propiedades de un fichero específico.
     *
     * @param fileNameWithoutExtension Nombre del fichero sin extensión.
     * @return Objeto {@code Properties} inmutable.
     * @throws PropertiesManagerException si el fichero no está disponible o el nombre es inválido.
     */
    Properties getProperties(String fileNameWithoutExtension) throws PropertiesManagerException;

    /**
     * Devuelve el valor de una clave buscando en el fichero, variables de entorno o propiedades del sistema.
     *
     * @param fileName Nombre del fichero sin extensión.
     * @param key Clave a buscar.
     * @return Valor encontrado o {@code null} si no existe.
     * @throws PropertiesManagerException si el nombre del fichero o la clave son inválidos.
     */
    String getProperty(String fileName, String key) throws PropertiesManagerException;

    /**
     * Devuelve un mapa inmutable con todos los ficheros de propiedades cargados.
     *
     * @return Mapa con el nombre del fichero como clave y sus {@code Properties} como valor.
     * @throws PropertiesManagerException si ocurre un error al acceder a los datos.
     */
    Map<String, Properties> getAllProperties() throws PropertiesManagerException;

    /**
     * Exporta las propiedades de un fichero específico en formato JSON.
     *
     * @param fileName Nombre del fichero sin extensión.
     * @param maskSensitiveValues Si {@code true}, se ocultan valores sensibles.
     * @return Cadena JSON con las propiedades.
     * @throws PropertiesManagerException si ocurre un error al procesar o exportar las propiedades.
     */
    String exportPropertiesToJson(String fileName, boolean maskSensitiveValues)
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
     * @param fileName Nombre del fichero sin extensión.
     * @param requiredKeys Conjunto de claves que deben existir.
     * @return {@code true} si todas las claves están presentes; {@code false} en caso contrario.
     * @throws PropertiesManagerException si el fichero es inválido o no se puede acceder.
     */
    boolean validateRequiredKeys(String fileName, Set<String> requiredKeys) throws PropertiesManagerException;

    /**
     * Recarga todas las propiedades desde el directorio configurado.
     *
     * @throws PropertiesManagerException si falla la recarga.
     */
    void reload() throws PropertiesManagerException;

    /**
     * Establece el directorio desde donde se cargarán los ficheros .properties.
     * Si es {@code null} o vacío, se usa la ruta por defecto.
     *
     * @param configDir Ruta del directorio.
     * @throws PropertiesManagerException si la ruta es inválida o no accesible.
     */
    void setConfigDir(String configDir) throws PropertiesManagerException;

    /**
     * Obtiene el directorio actualmente configurado para la carga de ficheros.
     *
     * @return Ruta del directorio de configuración.
     * @throws PropertiesManagerException si ocurre un error al acceder a la configuración.
     */
    String getConfigDir() throws PropertiesManagerException;

    /**
     * Establece una clave secreta para desencriptar valores sensibles.
     * Si es {@code null} o vacío, se establece una clave por defecto (modo desarrollo).
     *
     * @param key Clave secreta.
     * @throws PropertiesManagerException si la clave es inválida.
     */
    void setSecretKey(String key) throws PropertiesManagerException;

    /**
     * Obtiene la clave secreta actualmente definida.
     *
     * @return Clave secreta.
     * @throws PropertiesManagerException si no se puede acceder a la clave.
     */
    String getSecretKey() throws PropertiesManagerException;

    /**
     * Añade o reemplaza un conjunto de propiedades en memoria para un fichero específico.
     *
     * @param fileName Nombre del fichero sin extensión.
     * @param properties Propiedades a almacenar.
     * @throws PropertiesManagerException si los parámetros son inválidos o ocurre un error de almacenamiento.
     */
    void addProperties(String fileName, Properties properties) throws PropertiesManagerException;
}

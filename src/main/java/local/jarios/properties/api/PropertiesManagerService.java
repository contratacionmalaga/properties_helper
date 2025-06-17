package local.jarios.properties.api;

import local.jarios.properties.exception.PropertiesManagerException;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * <p>Interfaz que define las operaciones para la gestión centralizada de ficheros .properties.</p>
 *
 * <p>Esta API permite:
 * <ul>
 * <li>Carga de múltiples ficheros .properties desde una carpeta externa o el classpath.</li>
 * <li>Acceso inmutable a las propiedades para evitar modificaciones accidentales.</li>
 * <li>Ocultamiento de claves sensibles en operaciones de impresión y exportación.</li>
 * <li>Exportación de propiedades a formato JSON.</li>
 * <li>Validación de la presencia de claves requeridas en un fichero.</li>
 * <li>Recarga segura y sincronizada de todas las propiedades cargadas.</li>
 * </ul>
 *
 * @author Juan Antonio
 * @version 1.1
 * @since 2024-06-04
 */
public interface PropertiesManagerService {

    /**
     * Establece las claves que deben considerarse sensibles y, por lo tanto,
     * ser ocultadas (enmascaradas) durante la impresión o exportación.
     *
     * @param keys Un conjunto de cadenas que representan las claves sensibles.
     * Si es {@code null} o vacío, no se considerará ninguna clave como sensible.
     */
    void setSensitiveKeys(Set<String> keys);

    /**
     * Carga todos los ficheros .properties desde un directorio especificado.
     * Si el directorio no se encuentra o no es válido, intentará cargar recursos
     * predefinidos desde el classpath (normalmente, dentro del JAR).
     *
     * <p>Este método reemplaza completamente cualquier conjunto de propiedades cargadas previamente.</p>
     */
    void loadAllProperties();

    /**
     * Imprime en el log las propiedades de un fichero específico,
     * enmascarando los valores de las claves que han sido marcadas como sensibles.
     *
     * @param fileNameWithoutExtension El nombre del fichero de propiedades sin su extensión (ej. "app", "db").
     * @throws PropertiesManagerException
     * Si el fichero especificado no se encuentra entre las propiedades cargadas.
     */
    void printProperties(String fileNameWithoutExtension);

    /**
     * Imprime en el log todas las propiedades de todos los ficheros cargados,
     * aplicando el enmascaramiento para los valores sensibles.
     * Si no hay ficheros cargados, se registrará un mensaje informativo.
     */
    void printAllProperties();

    /**
     * Devuelve una copia inmutable de las propiedades asociadas a un fichero específico.
     * Las modificaciones a la {@code Properties} devuelta lanzarán una
     * {@code UnsupportedOperationException}.
     *
     * @param fileNameWithoutExtension El nombre del fichero de propiedades sin su extensión (ej. "app", "db").
     * @return Un objeto {@code Properties} inmutable que contiene las propiedades del fichero.
     * Devuelve un objeto {@code Properties} vacío si el fichero no existe entre los cargados.
     */
    Properties getProperties(String fileNameWithoutExtension);

    /**
     * Obtiene el valor de una clave específica. La búsqueda se realiza en el siguiente orden:
     * <ol>
     * <li>En el fichero de propiedades especificado.</li>
     * <li>En las variables de entorno del sistema.</li>
     * <li>En las propiedades del sistema Java.</li>
     * </ol>
     *
     * @param fileName El nombre del fichero de propiedades sin su extensión donde buscar primero.
     * @param key La clave cuyo valor se desea obtener.
     * @return El valor de la clave como {@code String}, o {@code null} si la clave no se encuentra
     * en ninguna de las fuentes.
     */
    String getProperty(String fileName, String key);

    /**
     * Devuelve un mapa inmutable que contiene todos los conjuntos de propiedades cargadas.
     * La clave del mapa es el nombre del fichero (sin extensión) y el valor es un objeto
     * {@code Properties} inmutable con las propiedades de ese fichero.
     *
     * @return Un {@code Map} inmutable de {@code String} a {@code Properties}.
     */
    Map<String, Properties> getAllProperties();

    /**
     * Exporta las propiedades de un fichero específico a una cadena en formato JSON.
     *
     * @param fileName El nombre del fichero de propiedades sin su extensión.
     * @param maskSensitiveValues Si es {@code true}, los valores de las claves sensibles
     * serán enmascarados en la salida JSON.
     * @return Una cadena JSON que representa las propiedades del fichero.
     * @throws PropertiesManagerException
     * Si el fichero no se encuentra o si ocurre un error durante la conversión a JSON.
     */
    String exportPropertiesToJson(String fileName, boolean maskSensitiveValues);

    /**
     * Exporta todas las propiedades cargadas (de todos los ficheros) a una cadena en formato JSON.
     * La salida JSON será un mapa donde cada clave es el nombre de un fichero
     * y su valor es un objeto JSON con las propiedades de ese fichero.
     *
     * @param maskSensitiveValues Si es {@code true}, los valores de las claves sensibles
     * serán enmascarados en la salida JSON.
     * @return Una cadena JSON que representa todas las propiedades cargadas.
     * @throws PropertiesManagerException
     * Si ocurre un error durante la conversión a JSON.
     */
    String exportAllPropertiesToJson(boolean maskSensitiveValues);

    /**
     * Valida que un fichero de propiedades específico contenga todas las claves requeridas.
     *
     * @param fileName El nombre del fichero de propiedades sin su extensión a validar.
     * @param requiredKeys Un conjunto de cadenas que representan las claves que deben estar presentes.
     * @return {@code true} si el fichero existe y contiene todas las claves requeridas;
     * {@code false} en caso contrario (fichero no encontrado o falta alguna clave).
     */
    boolean validateRequiredKeys(String fileName, Set<String> requiredKeys);

    /**
     * Recarga todas las propiedades desde el directorio de configuración actualmente establecido.
     * Este método es sincronizado para asegurar la consistencia.
     */
    void reload();

    /**
     * Establece la ruta del directorio desde donde se cargarán los ficheros .properties.
     * Si la ruta proporcionada es {@code null} o una cadena vacía/en blanco,
     * se restablecerá a la ruta de configuración por defecto.
     *
     * @param configDir La nueva ruta del directorio de configuración.
     */
    void setConfigDir(String configDir);

    /**
     * Obtiene la ruta del directorio desde donde se cargarán los ficheros .properties.
     * Si la ruta proporcionada es {@code null} o una cadena vacía/en blanco,
     * se restablecerá a la ruta de configuración por defecto.
     *
     * @return El fichero de configuración definido.
     */
    String getConfigDir();

    /**
     * Establece la clave secreta que se utilizará para desencriptar valores sensibles.
     * Si la clave proporcionada es {@code null} o una cadena vacía/en blanco,
     * se restablecerá a una clave por defecto (solo para desarrollo).
     *
     * @param key La nueva clave secreta.
     */
    void setSecretKey(String key);

    /**
     * Establece la clave secreta que se utilizará para desencriptar valores sensibles.
     * Si la clave proporcionada es {@code null} o una cadena vacía/en blanco,
     * se restablecerá a una clave por defecto (solo para desarrollo).
     *
     * @return La clave secreta definida.
     */
    String getSecretKey();
}
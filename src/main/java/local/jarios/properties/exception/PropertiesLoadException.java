package local.jarios.properties.exception;

/**
 * Excepción personalizada para errores relacionados con la carga
 * y manejo de ficheros de propiedades (.properties).
 * <p>
 * Esta excepción extiende de {@link RuntimeException} y se lanza cuando
 * ocurre cualquier fallo durante la lectura, validación o procesamiento
 * de ficheros properties en la aplicación.
 * </p>
 * <p>
 * Permite encapsular el mensaje de error y la causa raíz para facilitar
 * la depuración y trazabilidad.
 * </p>
 *
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 *     throw new PropertiesLoadException("Error cargando archivo properties");
 * </pre>
 *
 * @author Juan
 * @version 1.0
 * @since 2025-06-13
 */
public class PropertiesLoadException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje de error específico.
     *
     * @param message mensaje que describe la causa de la excepción
     */
    public PropertiesLoadException(String message) {
        super(message);
    }

    /**
     * Construye una nueva excepción con un mensaje de error específico
     * y una causa subyacente (otra excepción).
     *
     * @param message mensaje que describe la causa de la excepción
     * @param cause excepción original que provocó esta excepción
     */
    public PropertiesLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

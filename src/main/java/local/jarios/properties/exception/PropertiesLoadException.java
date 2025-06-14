package local.jarios.property.exception;

/**
 * Description:
 * Author: juan
 * Date: 13/06/2025
 * Team:
 */
public class PropertiesLoadException extends RuntimeException {
    public PropertiesLoadException(String message) {
        super(message);
    }

    public PropertiesLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

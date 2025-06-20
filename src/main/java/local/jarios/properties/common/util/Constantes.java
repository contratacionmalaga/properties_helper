package local.jarios.properties.common.util;

/**
 * Clase que contiene constantes generales utilizadas a lo largo de la aplicación.
 * <p>
 * Contiene cadenas comunes, formatos de fecha y caracteres de control,
 * para evitar el uso de valores mágicos en el código.
 * </p>
 *
 * <p><b>Author:</b> Juan Antonio</p>
 * <p><b>Date:</b> 04/06/2024</p>
 * <p><b>Team:</b> Juan Antonio</p>
 */
public final class Constantes {

    /** Directorio donde se encuentran los ficheros de config */
    public static final String DEFAULT_CONFIG_DIR = "config";

    /** Clave por defecto */
    public static final String DEFAULT_SECRET_KEY = "Malaga$$2025";

    /** Nombre sin extensión del fichero app.properties */
    public static final String APP_PROPERTIES = "app";

    /** Nombre sin extensión del fichero app.properties */
    public static final String APP_NON_EXISTS_PROPERTIES = "non.exists.file";

    /** Nombre sin extensión del fichero app.properties */
    public static final String KEY_APP_NAME = "app.name";

    /** Nombre sin extensión del fichero app.properties */
    public static final String KEY_NON_EXISTS = "non.exists.key";

    /** Nombre sin extensión del fichero email.properties */
    public static final String EMAIL_PROPERTIES = "email";

    /**
     * Valor que se usará para enmascarar claves sensibles durante la impresión o exportación.
     * Útil para ocultar contraseñas, tokens, etc.
     */
    public static final String KEY_SENSITIVE_VALUE = "******";

    /**
     * Extensión estándar usada para identificar ficheros de propiedades (.properties).
     * Se utiliza para filtrar archivos o construir rutas.
     */
    public static final String PROPERTIES_EXT = ".properties";

    /**
     * Constructor privado para evitar instanciación.
     */
    private Constantes() {
        // No instanciable
    }
}

package local.jarios.properties.common.util;

import java.util.Set;

/**
 * Clase que contiene constantes generales utilizadas a lo largo de la aplicación.
 *
 * <p>Contiene cadenas comunes, formatos de fecha y caracteres de control, para evitar el uso de
 * valores mágicos en el código.</p>
 *
 * @author Juan Antonio
 * @version 2.0
 * @since 2024-06-18
 */
public final class Constantes {

  /**
   * Ruta del directorio con los ficheros properties.
   */
  public static final String PROPERTIES_DIR = "properties";

  /**
   * Nombre sin extensión del fichero app.properties.
   */
  public static final String APP_PROPERTIES = "app";

  /**
   * Nombre sin extensión del fichero app.properties.
   */
  public static final String APP_NON_EXISTS_PROPERTIES = "non.exists.file";

  /**
   * Nombre sin extensión del fichero app.properties.
   */
  public static final String KEY_APP_NAME = "app.name";

  /**
   * Nombre sin extensión del fichero app.properties.
   */
  public static final String KEY_NON_EXISTS = "non.exists.key";

  /**
   * Nombre sin extensión del fichero email.properties.
   */
  public static final String EMAIL_PROPERTIES = "email";

  /**
   * Valor que se usará para enmascarar claves sensibles durante la impresión o exportación. Útil
   * para ocultar contraseñas, tokens, etc.
   */
  public static final String KEY_SENSITIVE_VALUE = "******";

  /**
   * Extensión estándar usada para identificar ficheros de propiedades (.properties). Se utiliza
   * para filtrar archivos o construir rutas.
   */
  public static final String PROPERTIES_EXT = ".properties";

  /**
   * Claves sensibles que se ocultan por defecto en logs y exportaciones.
   */
  public static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
      "password",
      "secret",
      "token",
      "apikey",
      "api_key",
      "credential"
  );

  /**
   * Constructor privado para evitar instanciación.
   */
  private Constantes() {
    // No instanciable
  }
}

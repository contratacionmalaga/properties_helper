package local.jarios.properties.helpers;

import java.util.Properties;

/**
 * Ayudante de los ficheros properties.
 */
public final class PropertiesHelper {

  /**
   * Constructro privado de la clase -- Evita es instanciamiento.
   */
  private PropertiesHelper() {

    // Constructor vacío
  }

  /**
   * Analiza si un fichero Properties es válido (comprueba que no sea null).
   *
   * @param properties Fichero con la ruta absoluta
   * @return Devuelve un valor indicando si el Properties es válido
   */
  public static boolean isPropertiesInvalid(Properties properties) {

    // Verificación de existencia del archivo
    return (properties == null) || (properties.isEmpty());
  }
}

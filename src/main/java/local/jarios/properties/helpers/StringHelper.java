package local.jarios.properties.helpers;

/**
 * Ayudante de los String.
 *
 * @author Juan Antonio
 */
public final class StringHelper {

  /**
   * Constructro privado de la clase -- Evita es instanciamiento.
   */
  private StringHelper() {

    // Constructor vacío
  }

  /**
   * Validador de cadena.
   *
   * @param cadena a validar
   * @return booleano indicando si la cadena es válida o no
   */
  public static boolean isStringInvalid(String cadena) {

    return ((cadena == null) || (cadena.isBlank()));
  }
}

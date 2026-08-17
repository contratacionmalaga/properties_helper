package local.jarios.properties.common.util;

/**
 * Clase final que contiene constantes de mensajes estáticos usados en la aplicación para logging y
 * trazabilidad.
 *
 * <p>Facilita la gestión centralizada de textos comunes para logs, evitando duplicación y
 * facilitando modificaciones.
 *
 * @author Juan Antonio
 * @version 2.0
 * @since 2024-06-18
 */
public final class Mensajes {

  /** Mensaje que indica el inicio de la ejecución del programa. */
  public static final String INICIO = "==== INICIO DE LA APLICACIÓN: properties-helper ====";

  /** Mensaje que indica el final de la ejecución del programa. */
  public static final String FINAL = "==== FINAL DE LA APLICACIÓN: properties-helper ====  ";

  /** Mensaje que indica que la ejecución ha finalizado correctamente. */
  public static final String FINAL_CORRECTO = "La ejecución ha finalizado CORRECTAMENTE.";

  /** Mensaje que indica que la ejecución ha finalizado con errores. */
  public static final String FINAL_ERROR = "!!!! La ejecución ha finalizado con ERRORES !!!!";

  /** Constructor privado para evitar la instanciación de esta clase de utilidades. */
  private Mensajes() {
    // Constructor privado
  }
}

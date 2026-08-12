package local.jarios.properties.helpers;

import local.jarios.properties.common.util.Mensajes;
import local.jarios.properties.enums.TipoFinalEjecucion;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase utilitaria para finalizar la ejecución del programa
 * registrando el resultado final mediante logs y terminando el proceso
 * con el código adecuado.
 */
@Slf4j
public final class FinalDelProgramaHelper {

  private FinalDelProgramaHelper() {
    // Constructor privado para evitar instanciación
  }

  /**
   * Finaliza la ejecución del programa.
   *
   * @param tipoFinal Tipo de finalización
   * @return código de salida asociado al tipo de finalización
   */
  public static int finalizar(TipoFinalEjecucion tipoFinal) {
    return finalizar(tipoFinal, null);
  }

  /**
   * Finaliza la ejecución del programa, registrando un mensaje adicional en caso de error.
   *
   * @param tipoFinal Tipo de finalización
   * @param mensajeError Mensaje opcional, solo usado si tipoFinal es ERROR
   * @return código de salida asociado al tipo de finalización
   */
  public static int finalizar(TipoFinalEjecucion tipoFinal, String mensajeError) {
    String mensaje;
    int exitCode;

    if (tipoFinal == TipoFinalEjecucion.CORRECTO) {
      mensaje = Mensajes.FINAL_CORRECTO;
      exitCode = 0;
    } else {
      mensaje = Mensajes.FINAL_ERROR;
      exitCode = 1;
      if (mensajeError != null && !mensajeError.isBlank()) {
        mensaje += " Detalle: " + mensajeError;
      }
    }

    log.info(mensaje);
    log.info(Mensajes.FINAL);

    System.out.flush();
    System.err.flush();

    return exitCode;
  }
}

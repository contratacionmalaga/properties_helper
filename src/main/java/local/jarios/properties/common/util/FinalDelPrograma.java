package local.jarios.properties.common.util;

import local.jarios.enums.TipoFinalEjecucion;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase utilitaria para finalizar la ejecución del programa
 * registrando el resultado final mediante logs y terminando el proceso
 * con el código adecuado.
 * <p>
 * El método {@code finalizar} acepta un tipo de finalización que determina
 * si la ejecución terminó correctamente o con error y actúa en consecuencia.
 * </p>
 *
 * <p><b>Author:</b> Juan Antonio</p>
 */
@Slf4j
public final class FinalDelPrograma {

    /**
     * Constructor privado para evitar instanciación.
     */
    private FinalDelPrograma() {
        /* CONSTRUCTOR VACÍO */
    }

    /**
     * Finaliza la ejecución del programa registrando un mensaje
     * de resultado y llamando a {@code System.exit} con el código
     * 0 para ejecución correcta o 1 para error.
     *
     * @param tipoFinal Tipo de finalización de la ejecución.
     */
    public static void finalizar(TipoFinalEjecucion tipoFinal) {
        String mensaje;
        int exitCode;

        if (tipoFinal == TipoFinalEjecucion.CORRECTO) {
            mensaje = Mensajes.FINAL_CORRECTO;
            exitCode = 0;
        } else {
            mensaje = Mensajes.FINAL_ERROR;
            exitCode = 1;
        }

        log.info(mensaje);
        log.info(Mensajes.FINAL);
        System.exit(exitCode);
    }
}

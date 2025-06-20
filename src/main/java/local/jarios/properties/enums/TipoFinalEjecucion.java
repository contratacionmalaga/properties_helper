package local.jarios.properties.enums;

import lombok.extern.slf4j.Slf4j;

/**
 * Enum que representa los posibles estados finales de ejecución del programa.
 * <p>
 * Este enum se utiliza para indicar si la ejecución del programa ha finalizado
 * de forma correcta o con errores, permitiendo realizar acciones específicas
 * según el estado final.
 * </p>
 * <p>
 * Autor: Juan Antonio Ríos Peláez<br>
 * Fecha: 03/03/2024<br>
 * Equipo: Contratación Electrónica
 * </p>
 */
@Slf4j
public enum TipoFinalEjecucion {

    /** Indica que la ejecución ha finalizado correctamente. */
    CORRECTO,

    /** Indica que la ejecución ha finalizado con errores. */
    ERROR;

    /**
     * Registra en los logs el estado final de la ejecución.
     * <p>
     * Este método permite registrar el estado final de la ejecución del programa
     * para su posterior análisis y seguimiento.
     * </p>
     *
     * @param tipoFinal El estado final de la ejecución.
     */
    public void registrarEstadoFinal(TipoFinalEjecucion tipoFinal) {
        switch (tipoFinal) {
            case CORRECTO:
                log.info("La ejecución del programa ha finalizado correctamente.");
                break;
            case ERROR:
                log.error("La ejecución del programa ha finalizado con errores.");
                break;
            default:
                log.warn("Estado final desconocido: {}", tipoFinal);
                break;
        }
    }
}

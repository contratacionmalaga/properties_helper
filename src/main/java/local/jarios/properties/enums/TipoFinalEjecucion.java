package local.jarios.properties.enums;

import lombok.extern.slf4j.Slf4j;

/**
 * Enum que representa los posibles estados finales de ejecución del programa.
 *
 * <p>Este enum se utiliza para indicar si la ejecución del programa ha finalizado de forma
 * correcta o con errores, permitiendo realizar acciones específicas según el estado final.</p>
 *
 * @author Juan Antonio
 * @version 2.0
 * @since 2024-06-18
 */
@Slf4j
public enum TipoFinalEjecucion {

  /**
   * Indica que la ejecución ha finalizado correctamente.
   */
  CORRECTO,

  /**
   * Indica que la ejecución ha finalizado con errores.
   */
  ERROR
}

package local.jarios.properties.common.util;

/**
 * Clase final que contiene constantes de mensajes estáticos
 * usados en la aplicación para logging y trazabilidad.
 * <p>
 * Facilita la gestión centralizada de textos comunes para logs,
 * evitando duplicación y facilitando modificaciones.
 * </p>
 *
 * <p><b>Author:</b> Juan Antonio</p>
 * <p><b>Date:</b> 04/06/2024</p>
 * <p><b>Team:</b> Juan Antonio</p>
 */
public class Mensajes {

    /**
     * Mensaje que indica el inicio de la ejecución del programa.
     */
    public static final String INICIO =
            "**** Inicio del log";

    /**
     * Mensaje que indica el inicio de la ejecución del programa.
     */
    public static final String FINAL =
            "**** Final del log";

    /**
     * Mensaje que indica que la ejecución ha finalizado correctamente.
     */
    public static final String FINAL_CORRECTO =
            "La ejecución ha finalizado CORRECTAMENTE.";

    /**
     * Mensaje que indica que la ejecución ha finalizado con errores.
     */
    public static final String FINAL_ERROR =
            "!!!! La ejecución ha finalizado con ERRORES !!!!";

    /**
     * Constructor privado para evitar la instanciación de esta clase de utilidades.
     */
    private Mensajes() {
        // Constructor privado
    }
}

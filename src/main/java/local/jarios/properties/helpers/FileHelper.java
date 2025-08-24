package local.jarios.properties.helpers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Ayudante de los files.
 *
 * @author Juan Antonio
 * @version 2.0
 * @since 2024-06-18
 */
public final class FileHelper {

  /**
   * LOGGER del componente.
   */
  private static final Logger LOGGER = LogManager.getLogger("local.jarios.properties");


  /**
   * Constructro privado de la clase -- Evita es instanciamiento.
   */
  private FileHelper() {

    // Constructor vacío
  }

  /**
   * Analiza si un String que se pasa es un File válido (EXISTE, SE PUEDA LEER, .entity..).
   *
   * @param file Fichero con la ruta absoluta
   * @return Devuelve un valor indicando si el fichero es valido
   */
  public static boolean isInvalidFile(File file) {

    if (file == null) {
      LOGGER.debug("[isInvalidFile] - El fichero es null.");
      return true;
    }

    if (!file.exists()) {
      LOGGER.debug("[isInvalidFile] - El fichero no existe: {}", file.getAbsolutePath());
      return true;
    }

    if (!file.isFile()) {
      LOGGER.debug("[isInvalidFile] - El fichero no es un fichero: {}", file.getAbsolutePath());
      return true;
    }

    if (!file.canRead()) {
      LOGGER.debug("[isInvalidFile] - El fichero no se puede leer: {}", file.getAbsolutePath());
      return true;
    }

    return false;
  }

  /**
   * Analiza si un String que se pasa es un File válido (EXISTE, SE PUEDA LEER, .entity..).
   *
   * @param directory Fichero con la ruta absoluta
   * @return Devuelve un valor indicando si el fichero es valido y en caso contrario indica el
   * motivo
   */
  public static boolean isInvalidDirectory(File directory) {

    if (directory == null) {
      LOGGER.debug("[isInvalidDirectory] - El directorio es null.");
      return true;
    }

    if (!directory.exists()) {
      LOGGER.debug("[isInvalidFile] - El directorio no existe: {}", directory.getAbsolutePath());
      return true;
    }

    if (!directory.isDirectory()) {
      LOGGER.debug("[isInvalidFile] - El directorio no es un Directorio: {}",
                   directory.getAbsolutePath());
      return true;
    }

    if (!directory.canRead()) {
      LOGGER.debug("[isInvalidFile] - El directorio no se puede leer: {}",
                   directory.getAbsolutePath());
      return true;
    }

    return false;
  }
}

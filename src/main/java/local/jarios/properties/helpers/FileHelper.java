package local.jarios.properties.helpers;

import lombok.extern.slf4j.Slf4j;
import java.io.File;

/**
 * Utilidades de ayuda para trabajar con {@link File}.
 *
 * <p>Proporciona métodos para validar ficheros y directorios.</p>
 *
 * @author Juan Antonio
 * @version 2.1
 * @since 2024-06-18
 */
@Slf4j
public final class FileHelper {

  private FileHelper() {
    // Constructor privado para evitar instanciación
  }

  /**
   * Verifica si un fichero es inválido.
   * <p>Un fichero es inválido si es null, no existe, no es un fichero o no se puede leer.</p>
   *
   * @param file Fichero a verificar
   * @return {@code true} si el fichero es inválido, {@code false} si es válido
   */
  public static boolean isInvalidFile(File file) {
    if (file == null) {
      log.debug("El fichero es null.");
      return true;
    }

    if (!file.exists()) {
      log.debug("El fichero no existe: {}", file.getAbsolutePath());
      return true;
    }

    if (!file.isFile()) {
      log.debug("El fichero no es un fichero: {}", file.getAbsolutePath());
      return true;
    }

    if (!file.canRead()) {
      log.debug("El fichero no se puede leer: {}", file.getAbsolutePath());
      return true;
    }

    return false;
  }

  /**
   * Verifica si un directorio es inválido.
   * <p>Un directorio es inválido si es null, no existe, no es un directorio o no se puede leer.</p>
   *
   * @param directory Directorio a verificar
   * @return {@code true} si el directorio es inválido, {@code false} si es válido
   */
  public static boolean isInvalidDirectory(File directory) {
    if (directory == null) {
      log.debug("El directorio es null.");
      return true;
    }

    if (!directory.exists()) {
      log.debug("El directorio no existe: {}", directory.getAbsolutePath());
      return true;
    }

    if (!directory.isDirectory()) {
      log.debug("El archivo no es un directorio: {}", directory.getAbsolutePath());
      return true;
    }

    if (!directory.canRead()) {
      log.debug("El directorio no se puede leer: {}", directory.getAbsolutePath());
      return true;
    }

    return false;
  }
}

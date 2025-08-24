package local.jarios.properties.helpers;

import java.util.Map;

/**
 * Ayudante de los maps.
 *
 * @author Juan Antonio
 */
public final class MapHelper {

  /**
   * Constructro privado de la clase -- Evita es instanciamiento.
   */
  private MapHelper() {

    // Constructor vacío
  }

  /**
   * Validador de maps.
   *
   * @param map mapa a validar
   * @param <K> Tipo asociado al Key del mapa
   * @param <V> Tipo asociado al Value del mapa
   * @return booleano con la respuesta
   */
  public static <K, V> boolean isMapInvalid(Map<K, V> map) {

    return (map == null);
  }
}

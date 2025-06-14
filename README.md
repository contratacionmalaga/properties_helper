# properties-helper

Gestión centralizada y segura de ficheros `.properties` en Java con funcionalidades avanzadas como carga desde carpeta externa o classpath, inmutabilidad, ocultamiento de claves sensibles, exportación a JSON y validación de claves requeridas.

---

## Descripción

`properties-helper` es una librería Java para manejar configuraciones almacenadas en ficheros `.properties` de forma centralizada, segura y flexible.

Implementa un **patrón Singleton thread-safe** que permite:

- Cargar múltiples ficheros `.properties` desde una carpeta externa `config` o desde los recursos del classpath (por ejemplo, empaquetados en el JAR).
- Mantener las propiedades inmutables para evitar modificaciones accidentales en tiempo de ejecución.
- Ocultar valores sensibles (como contraseñas o claves API) en impresión por consola o exportaciones.
- Exportar propiedades a formato JSON, con opción de enmascarar valores sensibles.
- Validar que un fichero `.properties` contenga claves obligatorias.
- Recargar todas las propiedades de forma sincronizada.

---

## Funcionalidades principales

- **Carga automática** de ficheros `.properties` en la carpeta `config/` o desde recursos empaquetados.
- **Inmutabilidad** de los objetos `Properties` para mayor seguridad.
- **Ocultamiento de claves sensibles** (`password`, `secret`, `token`, etc.).
- **Exportación a JSON** (individual o todas las propiedades).
- **Validación** de claves obligatorias.
- **Recarga** segura de configuraciones en caliente.
- Manejo de excepciones específicas con `PropertiesLoadException`.

---

## Estructura principal

- `PropertiesManager`: clase Singleton encargada de gestionar la carga, acceso, validación, exportación y recarga de propiedades.
- `PropertiesLoadException`: excepción personalizada para errores en carga o validación.
- `App`: clase ejemplo que muestra el uso del `PropertiesManager`.

---

## Uso básico desde otra aplicación

1. **Incluir el módulo en tu proyecto**
    - Agrega la dependencia (si está publicado en repositorio) o incluye el JAR generado en tu classpath.

2. **Obtener la instancia del `PropertiesManager` (Singleton)**

```java
public static void main(String[] args) {
   try {
      PropertiesManager manager = PropertiesManager.getInstance();

      // 1. Imprimir todas las properties cargadas (con ocultación de sensibles)
      log.info("---- Imprimiendo todas las propiedades ----");
      manager.printAllProperties();

      // 2. Imprimir propiedades de un fichero específico ("app")
      log.info("---- Propiedades del fichero 'app' ----");
      manager.printProperties("app");

      // 3. Obtener una propiedad concreta, con fallback a variables de entorno/sistema
      String dbUrl = manager.getProperty("db", "db.url");
      log.info("db.url = {}", dbUrl);

      // 4. Exportar propiedades a JSON ocultando valores sensibles
      String appJsonMasked = manager.exportAsJson("app", true);
      log.info("JSON exportado de 'app' con sensibles ocultos:\n{}", appJsonMasked);

      // 5. Exportar todas las propiedades a JSON sin ocultar (para ver diferencias)
      String allJson = manager.exportAllAsJson(false);
      log.info("JSON exportado de todas las propiedades (sin ocultar):\n{}", allJson);

      // 6. Validar que el fichero 'app' contenga claves obligatorias
      Set<String> requiredKeys = Set.of("app.name", "app.version");
      manager.validateRequiredKeys("app", requiredKeys);
      log.info("Validación de claves requeridas en 'app' completada OK.");

      // 7. Recargar propiedades (por si se modificaron los ficheros externos)
      manager.reload();
      log.info("Propiedades recargadas exitosamente.");

   } catch (PropertiesLoadException e) {
      log.error("Error gestionando propiedades", e);
   }
}
```
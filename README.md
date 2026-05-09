# properties-helper

Libreria Java para cargar, consultar, validar y exportar ficheros `.properties`
desde un directorio de configuracion.

## Requisitos

- Java 21.
- Maven 3.9.x o Maven Wrapper incluido en el repositorio.

## Instalacion local

```powershell
.\mvnw.cmd clean verify
```

En sistemas Unix:

```bash
./mvnw clean verify
```

## API principal

La entrada publica es `PropertiesManagerService`:

```java
PropertiesManagerService manager = PropertiesManagerServiceImpl.getInstance();
manager.setConfigDir("properties");
manager.loadAllProperties();
```

Los nombres de fichero pueden indicarse con o sin extension. Por ejemplo, `app`
y `app.properties` apuntan al mismo fichero logico.

## Uso basico

```java
import local.jarios.properties.api.PropertiesManagerService;
import local.jarios.properties.api.PropertiesManagerServiceImpl;
import local.jarios.properties.exception.PropertiesManagerException;

public class Example {

  public static void main(String[] args) {
    PropertiesManagerService manager = PropertiesManagerServiceImpl.getInstance();

    try {
      manager.setConfigDir("properties");
      manager.loadAllProperties();

      String appName = manager.getProperty("app", "app.name");
      String json = manager.exportPropertiesToJson("app.properties", true);

      System.out.println(appName);
      System.out.println(json);
    } catch (PropertiesManagerException ex) {
      System.err.println("Error cargando propiedades: " + ex.getMessage());
    }
  }
}
```

## Resolucion de propiedades

`getProperty(fileName, key)` busca en este orden:

1. Fichero `.properties` cargado.
2. Variable de entorno con el mismo nombre de clave.
3. Propiedad de sistema Java con el mismo nombre de clave.

Si la clave no existe en ningun origen, se lanza `PropertiesManagerException`.

## Seguridad y claves sensibles

La libreria enmascara por defecto claves cuyo nombre contenga:

- `password`
- `secret`
- `token`
- `apikey`
- `api_key`
- `credential`

El valor enmascarado se representa como `******`.

Se pueden reemplazar las claves sensibles:

```java
manager.setSensitiveKeys(Set.of("password", "private.key"));
```

Si se llama a `setSensitiveKeys(null)` o con un conjunto vacio, se restauran las
claves sensibles por defecto.

## Copias defensivas

Los metodos `getProperties` y `getAllProperties` devuelven copias defensivas.
Modificar los objetos devueltos no altera el estado interno del singleton.

## Operaciones disponibles

- `setConfigDir(String configDir)`
- `loadAllProperties()`
- `reload()`
- `getProperty(String fileName, String key)`
- `getProperties(String fileName)`
- `getAllProperties()`
- `addProperties(String fileName, Properties properties)`
- `setProperty(String fileName, String property, String value)`
- `hasLoaded(String fileName)`
- `getListFiles()`
- `validateRequiredKeys(String fileName, Set<String> requiredKeys)`
- `exportPropertiesToJson(String fileName, boolean maskSensitiveValues)`
- `exportAllPropertiesToJson(boolean maskSensitiveValues)`
- `setSensitiveKeys(Set<String> keys)`
- `getSensitiveKeys()`

## Calidad

Validacion basica:

```powershell
.\mvnw.cmd clean verify
```

Validacion de calidad estatica:

```powershell
.\mvnw.cmd -Pquality verify
```

El perfil `quality` ejecuta Checkstyle, SpotBugs y genera reporte JaCoCo.

## Publicacion

El proyecto esta configurado para publicar en GitHub Packages mediante
`distributionManagement`. Antes de publicar, verifica:

- version del artefacto en `pom.xml`;
- credenciales Maven para GitHub Packages;
- resultado correcto de `clean verify`;
- resultado correcto de `-Pquality verify`;
- ausencia de vulnerabilidades altas en dependencias runtime.

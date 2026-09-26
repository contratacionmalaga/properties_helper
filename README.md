# Properties Helper

Biblioteca Java para cargar, consultar, validar y gestionar ficheros `.properties`, con resolución alternativa desde variables de entorno y propiedades del sistema, exportación a JSON y enmascaramiento de valores sensibles.

**Artefacto Maven:** `local.jarios:properties-helper:6.1.0`
**Repositorio:** [contratacionmalaga/properties_helper](https://github.com/contratacionmalaga/properties_helper)

## Información de la versión

| Característica | Configuración |
|---|---|
| Versión | **6.1.0** |
| Tipo de artefacto | Biblioteca Java · JAR |
| Java de referencia y compilación | **21** |
| Maven mínimo | **3.9.16** |
| Maven distribuido mediante Wrapper | **3.9.16** |
| Maven Wrapper | **3.3.4** |
| Parent Maven | `local.jarios:jarios-parent:1.0.15` |
| Publicación | GitHub Packages |
| API principal | `PropertiesManagerService` |

El `pom.xml` y su parent son la referencia para las versiones de dependencias y herramientas de construcción. Los cambios de esta entrega se detallan en las [notas de la versión 6.1.0](docs/releases/6.1.0.md).

## Funcionalidades

- Carga de ficheros `.properties` desde un directorio configurable.
- Instancias independientes y acceso mediante singleton.
- Consulta de valores con resolución alternativa desde el entorno y la JVM.
- Validación de la presencia de claves obligatorias.
- Incorporación y modificación de propiedades en memoria.
- Recarga explícita de la configuración desde disco.
- Exportación de uno o varios conjuntos de propiedades a JSON.
- Enmascaramiento configurable de valores sensibles en logs y exportaciones.
- Copias defensivas para evitar modificaciones externas del estado interno.

## Requisitos

Para compilar el proyecto se necesita un JDK compatible con Java 21, `JAVA_HOME` configurado y acceso a los repositorios de dependencias.

El Maven Wrapper permite utilizar la versión de Maven configurada en el repositorio sin instalarla previamente. Su primera ejecución puede descargar Maven y los componentes necesarios.

Los artefactos alojados en GitHub Packages requieren credenciales con acceso de lectura a los paquetes correspondientes, incluido `jarios-parent`.

## Incorporación a un proyecto Maven

Añadir la dependencia al `pom.xml` del proyecto consumidor:

```xml
<dependency>
  <groupId>local.jarios</groupId>
  <artifactId>properties-helper</artifactId>
  <version>6.1.0</version>
</dependency>
```

Configurar el acceso a GitHub Packages en el `settings.xml` de Maven. El siguiente ejemplo utiliza variables de entorno para las credenciales:

```xml
<settings>
  <servers>
    <server>
      <id>github-properties-helper</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
    <server>
      <id>github-jarios-parent</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>jarios-packages</id>
      <repositories>
        <repository>
          <id>github-properties-helper</id>
          <url>https://maven.pkg.github.com/contratacionmalaga/properties_helper</url>
        </repository>
        <repository>
          <id>github-jarios-parent</id>
          <url>https://maven.pkg.github.com/contratacionmalaga/jarios-parent</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>jarios-packages</activeProfile>
  </activeProfiles>
</settings>
```

Si ya existe un `settings.xml`, integrar estos elementos en su configuración. Los identificadores de los servidores deben coincidir con los de los repositorios.

La biblioteca utiliza SLF4J como API de logging. La aplicación consumidora debe proporcionar una implementación compatible si necesita emitir logs. Logback se utiliza únicamente en las pruebas de este proyecto.

## Inicio rápido

Crear un directorio `properties` en el directorio de trabajo de la aplicación:

```text
properties/
└── app.properties
```

Contenido de `app.properties`:

```properties
app.name=Mi aplicacion
app.environment=development
service.token=example-token
```

Cargar y consultar la configuración:

```java
import java.util.Set;
import local.jarios.properties.api.PropertiesManagerService;
import local.jarios.properties.api.PropertiesManagerServiceImpl;
import local.jarios.properties.exception.PropertiesManagerException;

public class Application {

  public static void main(String[] args) {
    PropertiesManagerService manager =
        PropertiesManagerServiceImpl.newInstance();

    try {
      manager.setConfigDir("properties");
      manager.loadAllProperties();

      boolean valid =
          manager.validateRequiredKeys(
              "app", Set.of("app.name", "app.environment"));

      if (!valid) {
        throw new IllegalStateException("Faltan claves de configuración");
      }

      String applicationName = manager.getProperty("app", "app.name");
      String configurationJson = manager.exportPropertiesToJson("app", true);

      System.out.println(applicationName);
      System.out.println(configurationJson);
    } catch (PropertiesManagerException ex) {
      throw new IllegalStateException(
          "No se pudo inicializar la configuración", ex);
    }
  }
}
```

La exportación anterior sustituye el valor de `service.token` por `******`.

Los identificadores `app` y `app.properties` se refieren al mismo conjunto de propiedades.

## Modelo de instancias

### Instancias independientes

`newInstance()` crea un gestor con su propio estado:

```java
PropertiesManagerService manager =
    PropertiesManagerServiceImpl.newInstance();
```

Es la opción recomendada cuando distintos componentes o pruebas necesitan configuraciones independientes.

### Instancia compartida

`getInstance()` proporciona acceso al singleton:

```java
PropertiesManagerService manager =
    PropertiesManagerServiceImpl.getInstance();
```

Los consumidores de esa instancia comparten el directorio configurado, las propiedades cargadas y las reglas de enmascaramiento.

## Carga y resolución de valores

### Directorio de configuración

El directorio predeterminado es `properties`, relativo al directorio de trabajo del proceso.

```java
manager.setConfigDir("/ruta/configuracion");
manager.loadAllProperties();
```

- La carga examina los ficheros `.properties` del directorio indicado, sin recorrer subdirectorios.
- `setConfigDir(null)` y los valores en blanco restauran el directorio predeterminado.
- Cambiar el directorio no carga automáticamente su contenido.
- `loadAllProperties()` y `reload()` reemplazan el estado cargado cuando la lectura finaliza correctamente.
- Un directorio sin ficheros `.properties` deja el gestor vacío.
- Un directorio inexistente o inválido provoca `PropertiesManagerException`.

### Precedencia de consulta

`getProperty(fileName, key)` utiliza este orden:

1. Valor del fichero cargado.
2. Variable de entorno con el mismo nombre de clave.
3. Propiedad del sistema Java con el mismo nombre de clave.

La búsqueda alternativa también puede resolver una clave cuando el fichero no está cargado. Si ningún origen proporciona el valor, se lanza `PropertiesManagerException`.

Los nombres se consultan literalmente: `app.name` no se transforma automáticamente en `APP_NAME`. Un valor vacío presente en el fichero tampoco activa la búsqueda alternativa.

### Codificación de los ficheros

La implementación utiliza `Properties.load(InputStream)`, que interpreta los ficheros conforme al formato tradicional de Java Properties: ISO-8859-1 y secuencias de escape Unicode `\uXXXX`.

La codificación UTF-8 del proyecto Maven no modifica este comportamiento. Para caracteres fuera de ISO-8859-1, utilizar escapes Unicode.

## Validación de claves obligatorias

```java
boolean valid =
    manager.validateRequiredKeys(
        "app", Set.of("app.name", "app.environment"));
```

La validación comprueba la presencia de las claves en las propiedades cargadas del fichero. No consulta variables de entorno ni propiedades del sistema, ni valida el formato o contenido de los valores.

Con un estado cargado válido, un conjunto de claves `null` o vacío no impone requisitos adicionales.

## Modificación y recarga

```java
manager.setProperty("app", "app.environment", "production");
```

`setProperty()` crea el conjunto de propiedades si todavía no existe.

También es posible incorporar un objeto `Properties`:

```java
java.util.Properties properties = new java.util.Properties();
properties.setProperty("feature.enabled", "true");

manager.addProperties("features", properties);
```

`addProperties()` sustituye el conjunto existente con el mismo nombre y almacena una copia de las propiedades recibidas.

Estas operaciones modifican únicamente el estado en memoria. No escriben los cambios en los ficheros originales.

```java
manager.reload();
```

La recarga vuelve a leer el directorio configurado y reemplaza el estado en memoria, incluidos los cambios realizados mediante la API.

## Exportación y valores sensibles

Exportar un fichero:

```java
String json = manager.exportPropertiesToJson("app", true);
```

Exportar todos los ficheros cargados:

```java
String json = manager.exportAllPropertiesToJson(true);
```

Ambas operaciones devuelven JSON con formato legible. La exportación incluye las propiedades almacenadas en memoria; no incorpora valores resueltos exclusivamente desde el entorno o la JVM.

Con el enmascaramiento activado, se sustituyen por `******` los valores de claves cuyo nombre contiene alguno de estos fragmentos:

| Fragmento |
|---|
| `password` |
| `secret` |
| `token` |
| `apikey` |
| `api_key` |
| `credential` |

La comparación no distingue entre mayúsculas y minúsculas.

Para reemplazar los patrones:

```java
manager.setSensitiveKeys(
    java.util.Set.of("password", "private.key", "access.token"));
```

Pasar `null` o un conjunto vacío restaura los patrones predeterminados.

El enmascaramiento se aplica a la impresión de propiedades mediante los métodos de logging y a las exportaciones que lo soliciten. Las consultas de la API devuelven los valores originales. Exportar con `false` también devuelve los valores originales.

## Copias defensivas

`getProperties()` devuelve una copia del conjunto solicitado.

`getAllProperties()` devuelve un mapa no modificable cuyos valores son copias de los conjuntos almacenados.

Modificar estas copias no altera el estado del gestor. Para actualizarlo, utilizar `setProperty()` o `addProperties()`.

## Referencia de la API

| Operación | Finalidad |
|---|---|
| `getConfigDir()` | Consultar el directorio configurado |
| `setConfigDir(String)` | Establecer el directorio de carga |
| `loadAllProperties()` | Cargar los ficheros del directorio |
| `reload()` | Recargar la configuración desde disco |
| `getProperty(String, String)` | Resolver una propiedad |
| `getProperties(String)` | Obtener una copia de un conjunto |
| `getAllProperties()` | Obtener copias de todos los conjuntos |
| `addProperties(String, Properties)` | Incorporar o reemplazar un conjunto |
| `setProperty(String, String, String)` | Actualizar un valor en memoria |
| `hasLoaded(String)` | Comprobar si un conjunto está cargado |
| `getListFiles()` | Listar los identificadores cargados |
| `validateRequiredKeys(String, Set<String>)` | Comprobar claves obligatorias |
| `exportPropertiesToJson(String, boolean)` | Exportar un conjunto a JSON |
| `exportAllPropertiesToJson(boolean)` | Exportar todos los conjuntos a JSON |
| `setSensitiveKeys(Set<String>)` | Reemplazar los patrones sensibles |
| `getSensitiveKeys()` | Consultar los patrones sensibles |
| `printProperties(String)` | Registrar un conjunto a nivel DEBUG |
| `printAllProperties()` | Registrar todos los conjuntos a nivel DEBUG |

Las operaciones que requieren un mapa cargado válido pueden lanzar `PropertiesManagerException` cuando el gestor está vacío, incluidos `hasLoaded()` y `getListFiles()`.

Si existe un mapa cargado válido pero el fichero solicitado no está presente, `getProperties()` devuelve un conjunto vacío.

## Dependencias

Las versiones se gestionan mediante `jarios-parent`.

| Dependencia directa | Versión | Ámbito |
|---|---|---|
| SLF4J API | 2.0.20 | Compilación y ejecución |
| Jackson Databind | 2.22.3 | Compilación y ejecución |
| Lombok | 1.18.48 | `provided` |
| Logback Classic | 1.6.4 | Pruebas |
| JUnit Jupiter | 6.1.3 | Pruebas |
| AssertJ Core | 3.27.7 | Pruebas |

## Compilación y pruebas

Windows:

```powershell
.\mvnw.cmd clean verify
```

Linux y macOS:

```bash
./mvnw clean verify
```

Para instalar el artefacto en el repositorio Maven local:

```powershell
.\mvnw.cmd clean install
```

En Linux y macOS, utilizar `./mvnw` con los mismos argumentos.

El empaquetado genera los artefactos en `target/`, incluidos:

```text
properties-helper-6.1.0.jar
properties-helper-6.1.0-sources.jar
properties-helper-6.1.0-javadoc.jar
```

## Calidad y análisis de dependencias

Ejecutar las comprobaciones del perfil `quality`:

```powershell
.\mvnw.cmd -Pquality clean verify
```

El perfil combina la configuración del proyecto y del parent:

| Herramienta | Comprobación |
|---|---|
| Spotless | Formato del código |
| Checkstyle | Reglas de estilo |
| SpotBugs | Análisis estático |
| JaCoCo | Informe y umbral de cobertura |

El umbral configurado de JaCoCo es del **70 % de cobertura de instrucciones** para el conjunto del proyecto. Este valor es un requisito de construcción, no una declaración de la cobertura obtenida.

El informe HTML de cobertura se genera en `target/site/jacoco/index.html`.

OWASP Dependency-Check se ejecuta de forma independiente:

```powershell
.\mvnw.cmd org.owasp:dependency-check-maven:check
```

La configuración heredada utiliza `NVD_API_KEY` y establece un umbral de fallo CVSS de `8.0`. Este análisis no forma parte de `clean verify`.

## Integración continua

| Workflow | Finalidad |
|---|---|
| `CI` | Compilar y ejecutar pruebas |
| `Quality` | Ejecutar el perfil de calidad |
| `Dependency Check` | Analizar vulnerabilidades de dependencias |
| `Release Package` | Publicar el paquete y los artefactos de la release |

Los workflows de CI y calidad se ejecutan en los cambios y pull requests dirigidos a `main`, y permiten ejecución manual.

El análisis de dependencias dispone de ejecución programada y manual. Dependabot comprueba Maven diariamente y GitHub Actions semanalmente.

## Publicación de versiones

El workflow `Release Package` se activa al enviar un tag con formato `v<version>` o mediante ejecución manual indicando el tag.

Para la versión 6.1.0, el tag correspondiente es `v6.1.0`. El workflow comprueba que coincide con la versión declarada en el `pom.xml`.

El proceso:

1. Obtiene el código del tag.
2. Comprueba la correspondencia entre tag y versión Maven.
3. Ejecuta la compilación y las pruebas.
4. Publica el artefacto en GitHub Packages.
5. Crea la GitHub Release si todavía no existe.
6. Adjunta los JAR generados.

Antes de crear el tag deben haberse validado también el perfil `quality` y el análisis de dependencias, que son comprobaciones independientes del workflow de publicación.

## Mantenimiento

Los cambios deben mantener sincronizados el código, las pruebas, la documentación y la versión del artefacto.

Las actualizaciones de bibliotecas y herramientas comunes se gestionan preferentemente en `jarios-parent`. Cada actualización del parent debe validarse en este proyecto mediante las pruebas y los controles de calidad.

Las incidencias y propuestas de mejora pueden registrarse en [GitHub Issues](https://github.com/contratacionmalaga/properties_helper/issues), incluyendo la versión utilizada, el comportamiento esperado y un ejemplo mínimo reproducible.

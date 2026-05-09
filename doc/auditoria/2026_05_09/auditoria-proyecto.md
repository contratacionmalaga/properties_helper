# Auditoria del proyecto properties-helper

Fecha: 2026-05-09
Repositorio local: `C:\java\desarrollo\properties-helper`
Rama revisada: `main`

## 1. Resumen ejecutivo

El proyecto es una libreria Maven Java para gestionar ficheros `.properties`. La base de codigo es pequena y comprensible, pero el estado actual no es publicable con garantias: la configuracion de compilacion, CI, pruebas y documentacion no estan alineadas con la implementacion.

Riesgo global estimado: **medio-alto**.

Motivos principales:

- El `pom.xml` exige Java 21, pero el workflow de GitHub Actions configura JDK 17 y la maquina local expone Java 8.
- No hay pruebas automatizadas reales detectables con JUnit; existe una clase `PropertiesDemo` bajo `src/test`, pero no contiene metodos `@Test`.
- La API promete propiedades inmutables y fallback a variables de entorno/sistema, pero la implementacion no cumple esos contratos.
- `logback-classic` 1.5.24 arrastra `logback-core` 1.5.24, afectado por CVE-2026-1225 segun OSV/GitHub Advisory/Snyk.
- La configuracion de calidad existe, pero no queda integrada como puerta de calidad completa: Checkstyle y SpotBugs solo estan en perfil `quality` y no se pudieron ejecutar localmente.
- El README referencia nombres antiguos (`PropertiesManager`, `PropertiesLoadException`, `App`) que no existen en el codigo actual.

## 2. Alcance y metodologia

Se ha revisado:

- Estructura del repositorio y estado Git.
- `pom.xml`, dependencias, plugins y perfiles Maven.
- Workflow `.github/workflows/maven-ci.yml`.
- Codigo fuente en `src/main/java`.
- Recursos de ejemplo en `properties`.
- Recursos de test en `src/test`.
- Documentacion principal (`README.md`).
- Configuracion Qodana y Checkstyle.
- Senales basicas de secretos y credenciales.
- Estado local de compilacion y verificaciones ejecutables.
- Fuentes publicas de vulnerabilidades para dependencias principales.

No se ha realizado analisis dinamico completo porque `mvn` no esta disponible en el `PATH` local y el Java local encontrado es `1.8.0_491`, incompatible con la configuracion Java 21 del proyecto.

## 3. Estado del repositorio

El arbol de trabajo ya estaba modificado antes de crear esta auditoria:

- `pom.xml` modificado.
- `src/main/resources/spotbugs-security-exclude.xml` eliminado.
- `src/main/resources/spotbugs-security-include.xml` eliminado.
- `src/main/java/local/jarios/properties/PropertiesDemo.java` movido a `src/test/java/local/jarios/properties/PropertiesDemo.java`.
- `src/main/resources/logback.xml` movido/modificado a `src/test/resources/logback-test.xml`.
- `.github/workflows/` nuevo.
- `scripts_maven-import.ps1` nuevo.

Esta auditoria solo anade documentacion bajo `doc/auditoria/2026_05_09`.

## 4. Hallazgos criticos y altos

### A-001 - CI incompatible con el compilador configurado

Severidad: **alta**

Evidencia:

- `pom.xml:34` define `maven.compiler.source` como `21`.
- `pom.xml:143` exige Java 21 con `maven-enforcer-plugin`.
- `.github/workflows/maven-ci.yml:19` configura `java-version: '17'`.
- En local, `java -version` devuelve `1.8.0_491`.

Impacto:

El workflow `mvn clean verify` deberia fallar por incompatibilidad de Java. La validacion automatica no refleja el requisito real del proyecto y puede bloquear merges o releases.

Recomendacion:

Actualizar GitHub Actions a JDK 21 y documentar Java 21 como prerrequisito local. Si se quiere soportar Java 17, bajar `maven.compiler.source/target` y `requireJavaVersion` a 17 tras validar compatibilidad.

### A-002 - Sin pruebas automatizadas efectivas

Severidad: **alta**

Evidencia:

- Busqueda de `@Test`, `org.junit`, `assertThat` o `Assertions` en `src/test` y `src/main`: sin resultados.
- `src/test/java/local/jarios/properties/PropertiesDemo.java` contiene un `main`, no pruebas unitarias.
- El `pom.xml` declara JUnit 5 y AssertJ, pero no hay tests que los usen.

Impacto:

Cambios en carga, mutabilidad, enmascaramiento, exportacion JSON o concurrencia pueden romperse sin deteccion automatica.

Recomendacion:

Crear una bateria minima de tests JUnit 5 para:

- `loadAllProperties` con directorio valido, vacio e inexistente.
- `getProperty` para clave existente, inexistente y fichero inexistente.
- `setProperty` y `addProperties`.
- `validateRequiredKeys`.
- `exportPropertiesToJson` y `exportAllPropertiesToJson`.
- Enmascaramiento de claves sensibles.
- Inmutabilidad o, si no se quiere garantizar, ajustar el contrato publico.

### A-003 - Contrato de inmutabilidad incumplido

Severidad: **alta**

Evidencia:

- `PropertiesManagerService.java:75-81` documenta una copia inmutable.
- `PropertiesManagerServiceImpl.java:153-156` devuelve directamente el objeto `Properties` almacenado si existe.
- `PropertiesManagerServiceImpl.java:180-182` devuelve un mapa no modificable, pero los valores `Properties` internos siguen siendo mutables.
- `PropertiesManagerServiceImpl.java:96-101` guarda en el mapa la misma instancia recibida en `addProperties`.

Impacto:

Consumidores externos pueden modificar el estado interno del singleton sin pasar por validaciones, logging ni sincronizacion. Esto rompe el aislamiento de la libreria y puede provocar fallos dificiles de reproducir.

Recomendacion:

Devolver copias defensivas de cada `Properties` y almacenar copias al cargar/agregar. Para `getAllProperties`, devolver un mapa nuevo cuyas `Properties` tambien sean copias. Si se requiere inmutabilidad fuerte, exponer `Map<String, String>` inmutable en lugar de `Properties`.

### A-004 - Fallback a entorno/sistema documentado pero no implementado

Severidad: **alta**

Evidencia:

- `PropertiesManagerService.java:103-111` indica busqueda en fichero, variables de entorno o propiedades del sistema.
- `README.md:62` muestra el mismo contrato.
- `PropertiesManagerServiceImpl.java:160-170` solo consulta `props.getProperty(key)` y lanza excepcion si no existe.

Impacto:

Aplicaciones consumidoras pueden confiar en una funcionalidad inexistente para configuracion sensible o despliegues por entorno.

Recomendacion:

Implementar el fallback con orden explicito y documentado, por ejemplo: fichero -> `System.getenv(key)` -> `System.getProperty(key)`, o corregir la documentacion si no se desea ese comportamiento.

### A-005 - Vulnerabilidad en Logback transitivo

Severidad: **alta**

Evidencia:

- `pom.xml:43` fija `logback.version` en `1.5.24`.
- `logback-classic` arrastra `logback-core`.
- OSV/GitHub Advisory identifican CVE-2026-1225/GHSA-qqpg-mvqg-649v para Logback hasta 1.5.24.
- Snyk indica que `ch.qos.logback:logback-core@1.5.24` es vulnerable y recomienda subir a `1.5.25` o superior; tambien muestra `1.5.32` como version reciente/no vulnerable en la ficha consultada.

Impacto:

La vulnerabilidad requiere manipulacion del fichero de configuracion de Logback y clases presentes en classpath, pero afecta a una dependencia de runtime. En una libreria reutilizable, conviene no distribuir versiones vulnerables.

Recomendacion:

Actualizar Logback al menos a `1.5.25`; preferible validar con la ultima rama estable disponible en el momento del release. Reejecutar `mvn dependency:tree`, tests y un escaneo OWASP/OSV despues de actualizar.

Fuentes:

- OSV/GitHub Advisory: `https://osv.dev/vulnerability/GHSA-qqpg-mvqg-649v`
- Snyk Logback Core 1.5.24: `https://security.snyk.io/package/maven/ch.qos.logback%3Alogback-core/1.5.24`

## 5. Hallazgos medios

### A-006 - Jackson Core transitivo presenta avisos de seguridad en 2.20.1

Severidad: **media**

Evidencia:

- `pom.xml:48` fija `jackson.version` en `2.20.1`.
- `jackson-databind` 2.20.1 no muestra vulnerabilidades directas en Snyk, pero `jackson-core` 2.20.1 aparece con avisos altos de consumo de recursos en la ficha consultada.
- Snyk muestra `2.21.3` como version reciente/no vulnerable en la ficha general de `jackson-core`.

Impacto:

La libreria usa Jackson para serializar propiedades a JSON. Aunque la superficie de entrada es limitada, la dependencia transitiva deberia mantenerse en una version sin avisos conocidos.

Recomendacion:

Actualizar Jackson a una version no vulnerable alineada en todos sus modulos, preferiblemente mediante BOM de Jackson o gestion centralizada consistente.

Fuentes:

- Snyk Jackson Core: `https://security.snyk.io/package/maven/com.fasterxml.jackson.core%3Ajackson-core`
- Snyk Jackson Databind 2.20.1: `https://security.snyk.io/package/maven/com.fasterxml.jackson.core%3Ajackson-databind/2.20.1`

### A-007 - Calidad estatica configurada de forma incompleta

Severidad: **media**

Evidencia:

- `pom.xml:178-195` define perfil `quality` con SpotBugs y Checkstyle.
- No hay `executions` que enganchen esas herramientas al ciclo `verify`.
- Los filtros FindSecBugs referenciados historicamente aparecen eliminados del working tree.
- `qodana.yaml` usa solo `qodana.starter`, sin reglas especificas del proyecto.

Impacto:

El proyecto aparenta tener controles de calidad, pero no hay garantia de que se ejecuten en CI ni localmente de forma consistente.

Recomendacion:

Integrar `mvn -Pquality verify` en CI, restaurar o eliminar referencias obsoletas a filtros de seguridad y definir si Checkstyle debe fallar el build.

### A-008 - README desactualizado respecto al codigo

Severidad: **media**

Evidencia:

- `README.md:30` habla de `PropertiesLoadException`, pero el codigo tiene `PropertiesManagerException`.
- `README.md:36-38` referencia `PropertiesManager`, `PropertiesLoadException` y `App`, clases que no existen con esos nombres.
- El ejemplo usa metodos `exportAsJson` y `exportAllAsJson`, mientras la interfaz expone `exportPropertiesToJson` y `exportAllPropertiesToJson`.

Impacto:

Un consumidor no puede seguir el README para integrar la libreria sin descubrir diferencias por ensayo/error.

Recomendacion:

Reescribir el README a partir de la API actual y anadir un ejemplo compilable.

### A-009 - `PropertiesDemo` en tests puede terminar la JVM

Severidad: **media**

Evidencia:

- `FinalDelProgramaHelper.java:57` llama a `System.exit(exitCode)`.
- `PropertiesDemo.java` invoca `FinalDelProgramaHelper.finalizar(...)` desde el flujo principal.
- La clase esta bajo `src/test/java`.

Impacto:

Aunque no sea un test JUnit, si se ejecuta como parte de tareas auxiliares o demos automatizadas puede terminar el proceso Maven/IDE inesperadamente.

Recomendacion:

Separar demo ejecutable y pruebas automatizadas. Evitar `System.exit` en codigo reutilizable; devolver codigos o lanzar excepciones en capas invocables desde tests.

### A-010 - Posible inconsistencia de nombres logicos de fichero

Severidad: **media**

Evidencia:

- `loadAllProperties` guarda claves sin extension mediante `stripExtension`.
- `PropertiesDemo.java:402` usa `String testFile = "app.properties";` y luego llama `setProperty` y `hasLoaded` con ese nombre.

Impacto:

La API mezcla nombres con y sin extension. Esto puede crear entradas separadas (`app` y `app.properties`) para el mismo fichero conceptual.

Recomendacion:

Normalizar nombres de fichero en todos los metodos publicos, eliminando `.properties` si viene incluido, o rechazar explicitamente nombres con extension.

### A-011 - Mutacion parcial sin sincronizacion uniforme

Severidad: **media**

Evidencia:

- `loadAllProperties`, `reload` y `setSensitiveKeys` son `synchronized`.
- `setProperty` y `addProperties` modifican el mapa sin `synchronized`, aunque reasignan una referencia `volatile`.
- Los objetos `Properties` internos son mutables.

Impacto:

Puede haber condiciones de carrera si varios hilos modifican propiedades simultaneamente o si un consumidor muta un objeto devuelto por `getProperties`.

Recomendacion:

Sincronizar operaciones de escritura y aplicar copias defensivas. Si el objetivo es thread-safety real, cubrirlo con tests concurrentes basicos.

### A-012 - Script PowerShell con rutas absolutas y ejecucion dinamica

Severidad: **media**

Evidencia:

- `scripts_maven-import.ps1` fija `$basePath = "C:\ZZZ_Nubes\Mega\__DESARROLLO\JAVA\librerias"`.
- Ejecuta comandos mediante `Invoke-Expression`.

Impacto:

El script no es portable y `Invoke-Expression` aumenta el riesgo si se introducen valores no controlados.

Recomendacion:

Parametrizar rutas, documentar uso y sustituir `Invoke-Expression` por invocacion directa de `mvn` con argumentos validados.

## 6. Hallazgos bajos y mantenibilidad

### A-013 - Codificacion visible incorrecta en `.properties`

Severidad: **baja**

Evidencia:

- `properties/app.properties` y `properties/email.properties` muestran caracteres corruptos como `CONFIGURACI�N`.

Impacto:

Reduce legibilidad y puede generar salidas incorrectas si esos textos se consumen.

Recomendacion:

Normalizar archivos a UTF-8 y verificar `project.build.sourceEncoding`.

### A-014 - Textos y nombres copiados de otros proyectos

Severidad: **baja**

Evidencia:

- `Mensajes.java` usa `version-helper` en mensajes de inicio/fin, aunque el proyecto se llama `properties-helper`.

Impacto:

Confunde logs y trazabilidad en aplicaciones que consuman la libreria.

Recomendacion:

Actualizar mensajes y constantes para reflejar el nombre real del proyecto.

### A-015 - `TipoFinalEjecucion` tiene `@Slf4j` sin uso

Severidad: **baja**

Evidencia:

- `TipoFinalEjecucion.java` importa y aplica `@Slf4j`, pero no usa logger.

Impacto:

Ruido menor y dependencia innecesaria de Lombok en un enum simple.

Recomendacion:

Eliminar `@Slf4j` e import asociado.

### A-016 - Validacion de mapa demasiado laxa

Severidad: **baja**

Evidencia:

- `MapHelper.isMapInvalid` solo valida `null`.
- `propertiesMap` se inicializa con `Collections.emptyMap()`, por lo que muchos metodos operan sobre mapa vacio y fallan despues con errores menos especificos.

Impacto:

La semantica de "no cargado", "vacio" y "invalido" no es clara.

Recomendacion:

Definir comportamiento esperado: permitir mapa vacio sin excepcion, o lanzar mensajes especificos cuando no se haya llamado a `loadAllProperties`.

## 7. Seguridad de secretos y configuracion

Resultado:

- No se han encontrado tokens reales evidentes.
- `properties/email.properties` contiene valores de ejemplo (`mail.user=user`, `mail.password=password`), pero aun asi el nombre de clave es sensible.
- El README y el codigo tratan el enmascaramiento, pero la seguridad depende de que el consumidor configure `sensitiveKeys`; por defecto es `Collections.emptySet()`.

Riesgo:

Si el consumidor no llama a `setSensitiveKeys`, `printProperties` y exportaciones con mascara no ocultaran nada.

Recomendacion:

Definir claves sensibles por defecto (`password`, `secret`, `token`, `apikey`, `api_key`, `credential`, etc.) y permitir ampliarlas o sustituirlas explicitamente.

## 8. Build, empaquetado y releases

Observaciones:

- El `pom.xml` actual es mas simple que el historico y ha eliminado plugins de cobertura, release, fuentes, javadoc, shade, OWASP Dependency Check y FindSecBugs.
- `distributionManagement` apunta a GitHub Packages.
- El artifactId cambio de `properties_helper` a `properties-helper`, lo cual puede romper consumidores que dependan del artefacto anterior.
- No hay Maven Wrapper (`mvnw`) en el repositorio.

Recomendaciones:

- Anadir Maven Wrapper para reproducibilidad.
- Decidir si el cambio de `artifactId` es intencionado y documentar migracion.
- Restaurar generacion de sources/javadocs si la libreria se publica para consumo externo.
- Definir un perfil de release separado del perfil local.

## 9. Verificaciones ejecutadas

Comandos ejecutados:

- `rg --files`
- `git status --short`
- `git branch --show-current`
- `git log --oneline -5`
- `rg` sobre patrones de secretos y TODO/FIXME.
- `rg` sobre pruebas JUnit/aserciones.
- `java -version`
- `mvn test`
- `mvn -Pquality checkstyle:check spotbugs:check`

Resultados:

- `mvn test`: no ejecutado realmente; fallo porque `mvn` no esta disponible en el `PATH`.
- `mvn -Pquality checkstyle:check spotbugs:check`: mismo fallo, `mvn` no disponible.
- `java -version`: Java 8 (`1.8.0_491`), incompatible con Java 21.
- No hay reportes `target/surefire-reports`, `target/spotbugsXml.xml` ni `target/checkstyle-result.xml`.
- Existe `target/properties-helper-5.2.0.jar`, generado previamente el 2026-01-16, pero no se ha podido reproducir en esta auditoria.

## 10. Plan de remediacion recomendado

Prioridad 1:

1. Alinear Java local, Maven y CI a Java 21.
2. Actualizar Logback a version no vulnerable.
3. Crear Maven Wrapper.
4. Anadir tests JUnit 5 reales para la API publica.
5. Corregir contrato de inmutabilidad o la documentacion.

Prioridad 2:

1. Actualizar Jackson a una version sin avisos transitivos conocidos.
2. Integrar `mvn verify` y `mvn -Pquality verify` en CI.
3. Reescribir README con nombres y metodos actuales.
4. Normalizar nombres de fichero con/sin extension.
5. Evitar `System.exit` en codigo reutilizable.

Prioridad 3:

1. Limpiar anotaciones/imports no usados.
2. Corregir textos heredados de `version-helper`.
3. Normalizar codificacion UTF-8 de archivos `.properties`.
4. Parametrizar o retirar scripts locales no portables.

## 11. Conclusion

El proyecto tiene una API pequena y una intencion clara, pero ahora mismo su mayor riesgo no esta en complejidad tecnica sino en falta de coherencia: contrato publico, documentacion, CI, entorno Java y pruebas no coinciden. La remediacion deberia empezar por reproducibilidad del build y tests automatizados; despues conviene cerrar las brechas de seguridad de dependencias y endurecer la inmutabilidad/thread-safety prometida por la libreria.

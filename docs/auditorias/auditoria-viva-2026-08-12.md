# Auditoria viva del proyecto properties-helper

Fecha de creacion: 2026-08-12  
Ultima actualizacion: 2026-08-12  
Repositorio local: `C:\java\desarrollo\properties-helper`  
Version revisada: `6.0.0`  
Estado global: **riesgo bajo-controlado, con decisiones pendientes de versionado mayor**

## Como mantener viva esta auditoria

Este documento debe actualizarse en cada hito cerrado. Regla practica:

1. Cambiar el estado del hito o hallazgo.
2. Anadir fecha, evidencia y commit/PR si existe.
3. Mover hallazgos cerrados a "Cerrados / verificados".
4. Reejecutar las verificaciones indicadas antes de marcar un hito como cerrado.

Estados permitidos:

| Estado | Significado |
| --- | --- |
| Pendiente | No iniciado |
| En curso | Hay trabajo activo |
| Bloqueado | Necesita decision, credenciales o cambio externo |
| Cerrado | Implementado y verificado |
| Aceptado | Riesgo conocido que se decide no corregir ahora |

## Resumen ejecutivo

El proyecto ha mejorado de forma clara respecto a la auditoria historica de
`doc/auditoria/2026_05_09`: ya existe Maven Wrapper, el entorno local compila
con Java 21, el CI usa JDK 21, hay tests JUnit 6 reales, el perfil `quality`
ejecuta SpotBugs, Checkstyle y JaCoCo, y se han corregido problemas importantes
del contrato publico como copias defensivas, normalizacion de nombres,
fallback a entorno/sistema y claves sensibles por defecto.

La principal debilidad actual ya no es de bloqueo funcional, sino de mantenibilidad y gobierno tecnico: quedan decisiones pendientes sobre la semantica de `getProperties` para ficheros no cargados, la estrategia de migracion a Java 25 LTS y el uso futuro de Maven 4 en una fase separada.

## Verificaciones ejecutadas

| Verificacion | Resultado | Evidencia |
| --- | --- | --- |
| `.\mvnw.cmd -version` | OK | Maven 3.9.16, Java 21.0.9 |
| `.\mvnw.cmd clean verify` | OK | 14 tests, 0 fallos; genera JAR, sources JAR y Javadoc JAR |
| `.\mvnw.cmd -Pquality verify` | OK | 14 tests, SpotBugs 0 bugs, Checkstyle 0 warnings y JaCoCo check cumplido |
| `.\mvnw.cmd dependency:tree` | OK | Arbol de dependencias resuelto |
| `.\mvnw.cmd versions:display-dependency-updates` | OK | Solo quedan pre-release: AssertJ M1 y SLF4J alpha |
| `.\mvnw.cmd versions:display-plugin-updates` | OK | Solo queda Maven 4 beta/RC |

## Hallazgos cerrados / verificados

| ID | Hallazgo historico | Estado | Evidencia actual |
| --- | --- | --- | --- |
| A-001 | Java/Maven/CI no alineados | Cerrado | `pom.xml` usa Java 21; wrapper ejecuta Java 21.0.9; CI configura `java-version: '21'` |
| A-002 | Sin tests automatizados reales | Cerrado parcial | Hay 14 tests JUnit 6 en `PropertiesManagerServiceImplTest` |
| A-003 | Copias defensivas incumplidas | Cerrado | `addProperties`, `getProperties`, `getAllProperties` usan copias defensivas |
| A-004 | Fallback documentado no implementado | Cerrado | `getProperty` busca fichero, entorno y propiedad del sistema |
| A-005 | Logback vulnerable historico | Cerrado respecto a 1.5.24 | Logback esta en `1.6.2` y en scope `test` |
| A-006 | Jackson historico desactualizado | Cerrado respecto a 2.20.1 | Jackson esta en `2.22.1` |
| A-010 | Nombres con/sin `.properties` inconsistentes | Cerrado | `normalizeFileName` centraliza la normalizacion |
| A-011 | Escrituras sin sincronizacion uniforme | Cerrado parcial | `addProperties`, `setProperty`, `loadAllProperties`, `reload` estan sincronizados |
| A-012 | Script con ruta absoluta e `Invoke-Expression` | Cerrado | `scripts_maven-import.ps1` recibe `BasePath` y usa `& mvn @mavenArgs` |

## Hallazgos abiertos

### AV-001 - Checkstyle informa warnings pero no bloquea el build

Severidad: media  
Estado: Cerrado  
Ubicacion: `src/main/java/local/jarios/properties/api/PropertiesManagerServiceImpl.java`, `FinalDelProgramaHelper.java`, `FileHelper.java`

Evidencia:

- Se corrigieron warnings de orden de imports, longitud de linea, llaves, indentacion y Javadoc.
- `.\mvnw.cmd -Pquality verify` finaliza en `BUILD SUCCESS` sin warnings de Checkstyle ni Javadoc.
- SpotBugs informa 0 bugs y JaCoCo cumple el umbral configurado.

Impacto:

La puerta de calidad queda limpia para los cambios actuales.

Recomendacion:

Mantener `.\mvnw.cmd -Pquality verify` como validacion obligatoria antes de release.
### AV-002 - Dependencias directas con actualizaciones disponibles

Severidad: media  
Estado: Cerrado parcial

Resultado de `versions:display-dependency-updates` tras los cambios:

| Dependencia | Actual | Disponible | Recomendacion |
| --- | ---: | ---: | --- |
| `ch.qos.logback:logback-classic` | 1.6.2 | Sin update estable pendiente | Cerrado; queda en scope `test` |
| `com.fasterxml.jackson.core:jackson-databind` | 2.22.1 | Sin update pendiente | Cerrado |
| `org.projectlombok:lombok` | 1.18.46 | Sin update pendiente | Cerrado |
| `org.junit.jupiter:junit-jupiter-api` | 6.1.3 | Sin update pendiente | Cerrado; validado con `-Pquality verify` |
| `org.junit.jupiter:junit-jupiter-engine` | 6.1.3 | Sin update pendiente | Cerrado; validado con `-Pquality verify` |
| `org.assertj:assertj-core` | 3.27.7 | 4.0.0-M1 | No recomendado; es milestone |
| `org.slf4j:slf4j-api` | 2.0.17 | 2.1.0-alpha1 | No recomendado; es alpha |
### AV-003 - Plugins Maven con actualizaciones disponibles

Severidad: baja-media  
Estado: Cerrado parcial

Resultado de `versions:display-plugin-updates` tras los cambios:

| Plugin | Actual | Disponible | Recomendacion |
| --- | ---: | ---: | --- |
| `com.github.spotbugs:spotbugs-maven-plugin` | 4.10.3.0 | Sin update pendiente | Cerrado |
| `maven-compiler-plugin` | 3.15.0 | Maven 4 beta/RC | No migrar sin fase separada |
| `maven-dependency-plugin` | 3.11.0 | Sin update pendiente | Cerrado |
| `maven-enforcer-plugin` | 3.6.3 | Sin update pendiente | Cerrado |
| `org.codehaus.mojo:versions-maven-plugin` | 2.21.0 | Sin update pendiente | Cerrado |
| `org.jacoco:jacoco-maven-plugin` | 0.8.15 | Sin update pendiente | Cerrado |
| `maven-surefire-plugin` | 3.5.5 | 3.6.0-M1 | No recomendado salvo necesidad; milestone |

Nota: el informe tambien muestra plugins Maven 4 beta/RC, pero no conviene migrar a Maven 4 para este proyecto sin una fase separada.
### AV-004 - Maven Wrapper no esta en la ultima version 3.x

Severidad: baja  
Estado: Cerrado  
Ubicacion: `.mvn/wrapper/maven-wrapper.properties`

Evidencia:

- El wrapper usa Apache Maven `3.9.16`.
- `.\mvnw.cmd -version` confirma Maven 3.9.16 y Java 21.0.9.
- `clean verify` y `-Pquality verify` pasan con esta version.

Recomendacion:

Mantener el wrapper actualizado en la rama 3.9.x hasta que Maven 4 sea una decision separada.

Fuente:

- Apache Maven 3.9.16 release notes: https://maven.apache.org/docs/3.9.16/release-notes.html
### AV-005 - Java 21 sigue soportado, pero ya no es el LTS mas reciente

Severidad: baja-media  
Estado: Pendiente de decision  
Ubicacion: `pom.xml`

Evidencia:

- El proyecto compila con Java 21.
- Oracle lista JDK 26 como ultima version de la plataforma Java SE.
- Oracle lista JDK 25 como ultimo LTS y Java 21 como LTS anterior.
- Oracle indica `21.0.12` como version actual de la linea Java 21; localmente se
  usa `21.0.9`.

Recomendacion:

No migrar automaticamente el `maven.compiler.source` a 25/26 si la libreria
busca compatibilidad amplia. Si se mantiene Java 21, actualizar el JDK local/CI
a la ultima patch disponible de Java 21. Abrir una decision tecnica para evaluar
Java 25 LTS en una version mayor futura.

Fuentes:

- Oracle Java Downloads: https://www.oracle.com/java/technologies/downloads/
- Oracle Java SE at a Glance: https://www.oracle.com/java/technologies/java-se-glance.html
- Oracle Java SE Support Roadmap: https://www.oracle.com/java/technologies/java-se-support-roadmap.html

### AV-006 - Cobertura sin umbral minimo publicado

Severidad: media  
Estado: Cerrado  
Ubicacion: `pom.xml`

Evidencia:

- JaCoCo genera reporte en `-Pquality verify`.
- Existe regla `jacoco:check` con umbral de instrucciones cubiertas `0.70`.
- La validacion del 2026-08-12 cumple el umbral.

Impacto:

La cobertura ya no puede bajar por debajo del umbral inicial sin romper el perfil `quality`.

Recomendacion:

Mantener el umbral inicial en 70% y subirlo cuando se amplien tests sobre helpers y casos de error.
### AV-007 - Singleton mutable dificulta aislamiento de tests y uso concurrente

Severidad: media  
Estado: Cerrado parcial  
Ubicacion: `PropertiesManagerServiceImpl`

Evidencia:

- `getInstance()` se mantiene para compatibilidad.
- Se añade `PropertiesManagerServiceImpl.newInstance()` para crear instancias aisladas.
- Se añade test `newInstanceDoesNotShareStateWithSingleton`.
- `.\mvnw.cmd -Pquality verify` pasa con 14 tests.

Impacto:

Los consumidores que necesiten aislamiento ya no dependen obligatoriamente del singleton compartido.

Recomendacion:

Mantener `getInstance()` como API compatible y documentar `newInstance()` como opcion para tests o consumidores que requieran estado aislado.
### AV-008 - `getProperties` devuelve propiedades vacias para fichero no cargado

Severidad: baja-media  
Estado: Pendiente de decision  
Ubicacion: `PropertiesManagerServiceImpl#getProperties`

Evidencia:

- `getProperties` usa `getOrDefault(..., new Properties())`.
- Otros metodos como `exportPropertiesToJson` lanzan excepcion si el fichero no
  esta cargado.

Impacto:

La semantica es inconsistente: consultar propiedades de un fichero inexistente
parece correcto aunque no lo sea.

Recomendacion:

Decidir contrato: devolver vacio de forma explicita y documentada, o lanzar
`PropertiesManagerException` para alinearlo con el resto de metodos.

### AV-009 - Publicacion sin Javadoc JAR

Severidad: baja  
Estado: Cerrado  
Ubicacion: `pom.xml`, `.github/workflows/release-package.yml`

Evidencia:

- El build genera JAR, sources JAR y Javadoc JAR.
- `maven-javadoc-plugin` queda configurado en `build.plugins`.
- El workflow de release adjunta `properties-helper-<version>-javadoc.jar` a GitHub Release.

Impacto:

La publicacion queda mas completa para consumidores de la libreria.

Recomendacion:

Mantener la generacion y adjunto de Javadoc JAR en releases.
### AV-010 - Dependabot esta configurado solo para Maven

Severidad: baja  
Estado: Cerrado  
Ubicacion: `.github/dependabot.yml`

Evidencia:

- Existe `package-ecosystem: "maven"` con periodicidad diaria.
- Existe `package-ecosystem: "github-actions"` con periodicidad semanal.

Recomendacion:

Mantener Dependabot para Maven y GitHub Actions.
### AV-011 - Dependencia runtime de logging en una libreria

Severidad: baja-media  
Estado: Cerrado  
Ubicacion: `pom.xml`

Evidencia:

- El proyecto declara `logback-classic` en scope `test`.
- `dependency:tree` confirma `logback-classic` y `logback-core` como dependencias de test.
- La libreria mantiene `slf4j-api` en scope compile.

Impacto:

El consumidor ya no recibe Logback impuesto por la libreria.

Recomendacion:

Mantener Logback fuera del classpath runtime de consumidores.

### AV-012 - OWASP no debe bloquear el CI principal del paquete sin secreto NVD

Severidad: baja-media  
Estado: Cerrado  
Ubicacion: `.github/workflows/maven-ci.yml`, `.github/workflows/owasp-dependency-check.yml`

Evidencia:

- El CI principal queda limitado a `clean verify` y `-Pquality verify`.
- El escaneo OWASP se mueve a un workflow separado, ejecutable manualmente y por programacion semanal.
- El workflow dedicado usa `secrets.NVD_API_KEY`, que es donde debe resolverse la dependencia externa.

Impacto:

La calidad bloqueante del paquete no depende de credenciales externas, pero el repositorio conserva una via profesional para escaneo de vulnerabilidades.

Recomendacion:

Mantener OWASP en workflow separado y activar alertas segun la politica de seguridad del repositorio.

## Actualizaciones recomendadas

Orden propuesto:

1. **H1 - Actualizaciones menores seguras**
   - Cerrado en `jarp/auditoria-remediacion`.
   - Maven Wrapper `3.9.15 -> 3.9.16`.
   - Jackson `2.21.3 -> 2.22.1`.
   - Lombok `1.18.42 -> 1.18.46`.
   - Logback `1.5.32 -> 1.6.2` y scope `test`.
   - Plugins estables actualizados: compiler, dependency, enforcer, versions, JaCoCo, SpotBugs.
   - Validado con `clean verify` y `-Pquality verify`.
2. **H2 - Actualizaciones con mas riesgo**
   - Cerrado: JUnit `5.14.1 -> 6.1.3`, validado con `-Pquality verify`.
   - Pendiente: Maven 4 beta/RC, en fase separada.
3. **H3 - No aplicar por ahora**
   - AssertJ `4.0.0-M1`, por ser milestone.
   - SLF4J `2.1.0-alpha1`, por ser alpha.
   - Surefire `3.6.0-M1`, por ser milestone; el estable `3.5.5` ya queda aplicado.
   - Maven 4 beta/RC, salvo prueba separada.

## Hitos vivos

| Hito | Objetivo | Estado | Criterio de cierre | Ultima evidencia |
| --- | --- | --- | --- | --- |
| H1 | Actualizaciones menores y wrapper | Cerrado | Builds verdes tras subir versiones estables | `clean verify` y `-Pquality verify` OK 2026-08-12 |
| H2 | Decisiones de majors/pre-releases | Cerrado parcial | JUnit 6 validado; Maven 4, AssertJ milestone, SLF4J alpha y Surefire milestone quedan no aplicados | Informes Maven 2026-08-12 |
| H3 | Checkstyle como puerta real | Cerrado | 0 warnings relevantes | `-Pquality verify` OK sin warnings 2026-08-12 |
| H4 | Cobertura minima | Cerrado | JaCoCo `check` con umbral inicial | Umbral 70% cumplido 2026-08-12 |
| H5 | API singleton y semantica de fichero ausente | Cerrado parcial | `newInstance()` implementado; queda decidir `getProperties` para fichero ausente | `-Pquality verify` OK con 14 tests 2026-08-12 |
| H6 | Publicacion y mantenimiento | Cerrado | Javadoc/release/Dependabot GitHub Actions definidos | Javadoc JAR, release upload, Dependabot Actions y workflow OWASP separado configurados 2026-08-12 |

## Checklist operativo

- [x] Actualizar Maven Wrapper a 3.9.16.
- [x] Actualizar Jackson a 2.22.1.
- [x] Actualizar Lombok a 1.18.46.
- [x] Actualizar plugins Maven estables.
- [x] Decidir si Logback debe ser runtime o test.
- [x] Evaluar Logback 1.6.2.
- [x] Evaluar JUnit 6.1.3 en rama separada.
- [ ] Mantener Java 21 o planificar Java 25 LTS.
- [ ] Actualizar JDK local/CI a ultima patch de Java 21 si se mantiene Java 21.
- [x] Corregir warnings de Checkstyle.
- [x] Decidir si Checkstyle debe fallar CI.
- [x] Anadir umbral JaCoCo.
- [ ] Decidir comportamiento de `getProperties` ante fichero no cargado.
- [x] Evaluar alternativa al singleton para instancias aisladas.
- [x] Configurar Javadoc JAR si la libreria se publica.
- [x] Anadir Dependabot para GitHub Actions.

## Comandos de mantenimiento

Ejecutar antes de cerrar cualquier hito:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pquality verify
.\mvnw.cmd dependency:tree
.\mvnw.cmd versions:display-dependency-updates
.\mvnw.cmd versions:display-plugin-updates
```

Para revisar Java y Maven:

```powershell
.\mvnw.cmd -version
Get-Content .mvn/wrapper/maven-wrapper.properties
```

## Registro de actualizaciones

| Fecha | Cambio | Estado | Evidencia |
| --- | --- | --- | --- |
| 2026-08-12 | Creacion de auditoria viva | Cerrado | Documento creado en `docs/auditorias` |
| 2026-08-12 | Rama `jarp/auditoria-remediacion` creada | Cerrado | `git switch -c jarp/auditoria-remediacion` |
| 2026-08-12 | H1 cerrado: wrapper, dependencias y plugins estables actualizados | Cerrado | `clean verify`, `dependency:tree`, `versions:*` OK |
| 2026-08-12 | H3 cerrado: Checkstyle/Javadoc sin warnings | Cerrado | `-Pquality verify` OK sin warnings |
| 2026-08-12 | H4 cerrado: JaCoCo check al 70% | Cerrado | `jacoco:check` cumplido |
| 2026-08-12 | H6 cerrado: Javadoc JAR, release upload y Dependabot GitHub Actions | Cerrado | `maven-javadoc-plugin`, release workflow y `.github/dependabot.yml` actualizados |
| 2026-08-12 | H2 cerrado parcial: JUnit 6.1.3 y Surefire 3.5.5 aplicados y validados | Cerrado parcial | `-Pquality verify`, `dependency:tree`, `versions:*` OK |
| 2026-08-12 | H5 cerrado parcial: instancia aislada sin romper singleton | Cerrado parcial | `newInstance()` y test dedicado; `-Pquality verify` OK con 14 tests |
| 2026-08-12 | CI corregido: workflow principal estable y OWASP separado | Cerrado | `.github/workflows/maven-ci.yml` usa `actions/checkout@v5`, `actions/setup-java@v5`; OWASP queda en `.github/workflows/owasp-dependency-check.yml` con `NVD_API_KEY` |




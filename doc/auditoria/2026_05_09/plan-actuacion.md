# Plan de actuacion sobre la auditoria

Fecha: 2026-05-09
Proyecto: `properties-helper`
Auditoria base: `doc/auditoria/2026_05_09/auditoria-proyecto.md`

## 1. Objetivo

Reducir el riesgo del proyecto de **medio-alto** a **bajo controlado**, dejando la libreria en un estado reproducible, verificable y coherente con su contrato publico.

El plan se organiza por fases para resolver primero los bloqueantes de build y seguridad, despues las garantias funcionales de la API, y finalmente la documentacion y limpieza de mantenibilidad.

## 2. Criterios de cierre global

El plan se considerara completado cuando:

- El proyecto compile localmente y en CI con la misma version de Java.
- `mvn verify` se ejecute correctamente.
- `mvn -Pquality verify` se ejecute correctamente o exista una decision documentada sobre que reglas quedan como informativas.
- Exista una suite minima de tests JUnit 5 para la API publica.
- Las dependencias con avisos conocidos hayan sido actualizadas o justificadas.
- README, ejemplos y contrato de la interfaz coincidan con la implementacion.
- Los hallazgos A-001 a A-016 esten cerrados, aceptados como riesgo o replanificados con motivo.

## 3. Fase 0 - Preparacion y control de cambios

Prioridad: **inmediata**
Duracion estimada: **0,5 dias**
Hallazgos relacionados: estado del repositorio, reproducibilidad

### Acciones

1. Crear una rama de trabajo especifica, por ejemplo `codex/auditoria-remediacion-2026-05-09`.
2. Revisar los cambios ya existentes en el working tree y decidir si forman parte de la remediacion o deben cerrarse aparte.
3. Confirmar si el cambio de `artifactId` de `properties_helper` a `properties-helper` es intencionado.
4. Definir version objetivo del proyecto tras remediacion.
5. Registrar en una tabla de seguimiento cada hallazgo con estado: pendiente, en curso, cerrado, riesgo aceptado.

### Entregables

- Rama de trabajo creada.
- Decision documentada sobre `artifactId`.
- Lista de hallazgos con estado inicial.

### Criterios de aceptacion

- No hay cambios mezclados sin clasificar.
- Se sabe si la remediacion mantiene compatibilidad con consumidores actuales.

## 4. Fase 1 - Reproducibilidad de build y CI

Prioridad: **critica**
Duracion estimada: **1 dia**
Hallazgos relacionados: A-001, build/releases

### Acciones

1. Instalar o configurar localmente JDK 21.
2. Instalar Maven o anadir Maven Wrapper al repositorio.
3. Actualizar `.github/workflows/maven-ci.yml` para usar JDK 21.
4. Cambiar actions obsoletas a versiones actuales cuando sea posible:
   - `actions/checkout@v4`
   - `actions/setup-java@v4`
   - `actions/cache@v4`, o cache integrada de `setup-java`.
5. Ejecutar localmente:
   - `mvn -version`
   - `mvn clean verify`
6. Confirmar que el workflow ejecuta la misma ruta que local.

### Entregables

- Maven Wrapper o documentacion explicita de Maven requerido.
- Workflow alineado con Java 21.
- Build reproducible.

### Criterios de aceptacion

- `mvn clean verify` termina correctamente en local.
- El CI deja de fallar por version de Java.
- La version de Java requerida aparece en README.

## 5. Fase 2 - Seguridad de dependencias

Prioridad: **critica**
Duracion estimada: **0,5 a 1 dia**
Hallazgos relacionados: A-005, A-006, seguridad de secretos

### Acciones

1. Actualizar Logback desde `1.5.24` a una version no vulnerable validada.
2. Actualizar Jackson desde `2.20.1` a una version sin avisos transitivos conocidos.
3. Ejecutar:
   - `mvn dependency:tree`
   - `mvn versions:display-dependency-updates`
   - `mvn clean verify`
4. Integrar un escaneo de dependencias en CI:
   - OWASP Dependency Check, OSV Scanner, Snyk o herramienta equivalente.
5. Definir claves sensibles por defecto en la libreria:
   - `password`
   - `secret`
   - `token`
   - `apikey`
   - `api_key`
   - `credential`
6. Mantener posibilidad de ampliar o reemplazar claves sensibles desde API.

### Entregables

- Dependencias actualizadas.
- Escaneo de dependencias integrado o documentado.
- Enmascaramiento seguro por defecto.

### Criterios de aceptacion

- No quedan avisos altos conocidos en dependencias runtime principales.
- El proyecto sigue compilando y pasando tests.
- Una prueba valida que `mail.password` queda enmascarado sin configuracion manual previa.

## 6. Fase 3 - Pruebas automatizadas minimas

Prioridad: **alta**
Duracion estimada: **1 a 2 dias**
Hallazgos relacionados: A-002, A-003, A-004, A-010, A-011

### Acciones

1. Crear tests JUnit 5 reales para `PropertiesManagerServiceImpl`.
2. Usar directorios temporales con `@TempDir` para no depender de `properties/` real.
3. Cubrir los siguientes casos:
   - carga de directorio valido con varios `.properties`;
   - directorio vacio;
   - directorio inexistente;
   - clave existente;
   - clave inexistente;
   - fichero inexistente;
   - `addProperties`;
   - `setProperty`;
   - `hasLoaded`;
   - `validateRequiredKeys`;
   - `exportPropertiesToJson`;
   - `exportAllPropertiesToJson`;
   - enmascaramiento de sensibles;
   - normalizacion de nombres con y sin `.properties`;
   - copia defensiva o inmutabilidad efectiva;
   - comportamiento concurrente basico si se mantiene promesa thread-safe.
4. Retirar o aislar `PropertiesDemo` para que no se confunda con tests.
5. Evitar que cualquier test pueda ejecutar `System.exit`.

### Entregables

- Suite JUnit 5 en `src/test/java`.
- Datos de test autocontenidos.
- `PropertiesDemo` separado como demo manual o eliminado del ciclo de tests.

### Criterios de aceptacion

- `mvn test` ejecuta tests reales.
- Los tests fallan si se rompe el contrato publico principal.
- No hay dependencia de rutas absolutas ni del directorio `properties/` del repo para pruebas unitarias.

## 7. Fase 4 - Correccion del contrato funcional de la API

Prioridad: **alta**
Duracion estimada: **1 a 2 dias**
Hallazgos relacionados: A-003, A-004, A-010, A-011, A-016

### Acciones

1. Decidir contrato definitivo:
   - opcion recomendada: mantener promesa de inmutabilidad y thread-safety.
2. Implementar copias defensivas:
   - al cargar desde fichero;
   - al recibir `addProperties`;
   - al devolver `getProperties`;
   - al devolver `getAllProperties`.
3. Normalizar nombres de fichero en una sola funcion interna:
   - aceptar `app` y `app.properties` como el mismo identificador; o
   - rechazar nombres con extension y documentarlo.
4. Implementar fallback documentado:
   - fichero;
   - variable de entorno;
   - propiedad del sistema.
5. Revisar semantica de mapa vacio:
   - distinguir "no cargado", "sin ficheros" y "fichero inexistente".
6. Sincronizar operaciones de escritura o sustituir estructura interna por una estrategia concurrente clara.

### Entregables

- Implementacion coherente con Javadocs.
- Tests actualizados para el contrato elegido.

### Criterios de aceptacion

- Un consumidor no puede mutar el estado interno a traves de objetos devueltos.
- `getProperty` cumple exactamente el orden de resolucion documentado.
- `app` y `app.properties` no generan entradas divergentes.

## 8. Fase 5 - Calidad estatica y cobertura

Prioridad: **media-alta**
Duracion estimada: **1 dia**
Hallazgos relacionados: A-007

### Acciones

1. Integrar Checkstyle en el ciclo `verify` o en `mvn -Pquality verify`.
2. Integrar SpotBugs en el mismo perfil.
3. Decidir si se restaura FindSecBugs.
4. Anadir JaCoCo si se quiere medir cobertura minima.
5. Definir umbral inicial realista:
   - recomendado inicial: 60-70%;
   - subir a 80% cuando la suite se estabilice.
6. Hacer que CI ejecute al menos:
   - `mvn clean verify`
   - `mvn -Pquality verify`

### Entregables

- Perfil `quality` operativo.
- CI ejecutando calidad estatica.
- Reportes generados en `target`.

### Criterios de aceptacion

- Checkstyle y SpotBugs se pueden ejecutar localmente.
- El CI falla ante errores de calidad definidos como bloqueantes.
- Las reglas informativas quedan documentadas.

## 9. Fase 6 - Documentacion y ejemplos

Prioridad: **media**
Duracion estimada: **0,5 a 1 dia**
Hallazgos relacionados: A-008, README, build/releases

### Acciones

1. Reescribir README usando nombres reales:
   - `PropertiesManagerService`
   - `PropertiesManagerServiceImpl`
   - `PropertiesManagerException`
2. Sustituir metodos inexistentes por metodos actuales:
   - `exportPropertiesToJson`
   - `exportAllPropertiesToJson`
3. Documentar requisitos:
   - Java 21;
   - Maven o Maven Wrapper;
   - formato esperado de ficheros `.properties`.
4. Anadir ejemplo compilable.
5. Documentar comportamiento de:
   - inmutabilidad;
   - fallback;
   - claves sensibles por defecto;
   - normalizacion de nombres.
6. Si el `artifactId` cambia, anadir seccion de migracion.

### Entregables

- README actualizado.
- Ejemplo de uso compilable.
- Tabla de compatibilidad/migracion si aplica.

### Criterios de aceptacion

- Un usuario puede integrar la libreria siguiendo solo el README.
- Los ejemplos compilan contra la API actual.

## 10. Fase 7 - Limpieza de mantenibilidad

Prioridad: **media-baja**
Duracion estimada: **0,5 a 1 dia**
Hallazgos relacionados: A-009, A-012, A-013, A-014, A-015

### Acciones

1. Corregir codificacion de `properties/app.properties` y `properties/email.properties` a UTF-8.
2. Cambiar textos heredados de `version-helper` a `properties-helper`.
3. Eliminar `@Slf4j` no usado en `TipoFinalEjecucion`.
4. Revisar `FinalDelProgramaHelper`:
   - moverlo a demo si no pertenece a la libreria;
   - evitar `System.exit` en codigo de libreria.
5. Parametrizar `scripts_maven-import.ps1`.
6. Sustituir `Invoke-Expression` por invocacion segura.
7. Decidir si el script debe permanecer en el repo.

### Entregables

- Codigo auxiliar limpio.
- Scripts portables o retirados.
- Recursos en UTF-8.

### Criterios de aceptacion

- No quedan textos de otros proyectos.
- No hay salida inesperada de JVM desde codigo reutilizable.
- El script no depende de rutas personales para ejecutarse correctamente.

## 11. Fase 8 - Release y validacion final

Prioridad: **final**
Duracion estimada: **0,5 dias**
Hallazgos relacionados: todos

### Acciones

1. Ejecutar validacion completa:
   - `mvn clean verify`
   - `mvn -Pquality verify`
   - escaneo de dependencias
2. Revisar `git diff`.
3. Generar changelog tecnico de remediacion.
4. Actualizar version si procede.
5. Crear tag o preparar release segun politica del proyecto.

### Entregables

- Build final verificado.
- Changelog de remediacion.
- Version lista para publicar o consumir internamente.

### Criterios de aceptacion

- CI en verde.
- No hay hallazgos altos abiertos sin riesgo aceptado.
- El paquete generado corresponde a la version esperada.

## 12. Matriz de trazabilidad

| Hallazgo | Fase principal | Resultado esperado |
| --- | --- | --- |
| A-001 | Fase 1 | Java local y CI alineados |
| A-002 | Fase 3 | Tests JUnit 5 reales |
| A-003 | Fase 4 | Copias defensivas / inmutabilidad real |
| A-004 | Fase 4 | Fallback implementado o documentacion corregida |
| A-005 | Fase 2 | Logback actualizado |
| A-006 | Fase 2 | Jackson actualizado |
| A-007 | Fase 5 | Perfil quality operativo |
| A-008 | Fase 6 | README coherente |
| A-009 | Fase 7 | Demo aislada y sin `System.exit` en libreria |
| A-010 | Fase 4 | Nombres de fichero normalizados |
| A-011 | Fase 4 | Escrituras sincronizadas / thread-safety definida |
| A-012 | Fase 7 | Script portable y sin `Invoke-Expression` |
| A-013 | Fase 7 | Recursos UTF-8 |
| A-014 | Fase 7 | Logs con nombre correcto |
| A-015 | Fase 7 | Anotacion no usada retirada |
| A-016 | Fase 4 | Semantica de estado vacio clara |

## 13. Orden recomendado de ejecucion

1. Fase 0 - Preparacion.
2. Fase 1 - Build y CI.
3. Fase 2 - Seguridad de dependencias.
4. Fase 3 - Tests.
5. Fase 4 - API y comportamiento.
6. Fase 5 - Calidad estatica.
7. Fase 6 - Documentacion.
8. Fase 7 - Limpieza.
9. Fase 8 - Release.

No conviene empezar por documentacion o limpieza antes de cerrar build, dependencias y tests, porque esos cambios no reducen el riesgo principal y pueden quedar obsoletos si se ajusta el contrato funcional.

## 14. Riesgos de ejecucion

| Riesgo | Impacto | Mitigacion |
| --- | --- | --- |
| Consumidores dependen del `artifactId` antiguo | Alto | Confirmar compatibilidad antes de publicar |
| Tests revelan cambios de contrato no previstos | Medio-alto | Decidir contrato explicito antes de implementar |
| Actualizacion de dependencias cambia comportamiento | Medio | Actualizar una familia de dependencias por commit y ejecutar tests |
| Checkstyle genera demasiadas incidencias iniciales | Medio | Activar primero en modo informe y endurecer por iteraciones |
| Fallback a variables de entorno introduce ambiguedad | Medio | Documentar orden exacto y cubrirlo con tests |

## 15. Propuesta de hitos

| Hito | Contenido | Estado objetivo |
| --- | --- | --- |
| H1 | Java 21, Maven Wrapper, CI corregido | Build reproducible |
| H2 | Logback/Jackson actualizados y escaneo integrado | Riesgo de dependencias reducido |
| H3 | Tests JUnit 5 para API publica | Cambios protegidos |
| H4 | Inmutabilidad, fallback y normalizacion cerrados | Contrato estable |
| H5 | Quality profile y README actualizados | Proyecto mantenible |
| H6 | Limpieza, changelog y release | Version lista |

## 16. Checklist operativo

- [ ] Crear rama de remediacion.
- [ ] Clasificar cambios locales existentes.
- [ ] Confirmar `artifactId` objetivo.
- [ ] Configurar JDK 21 local.
- [ ] Anadir Maven Wrapper o documentar Maven requerido.
- [ ] Actualizar workflow a JDK 21.
- [ ] Actualizar Logback.
- [ ] Actualizar Jackson.
- [ ] Integrar escaneo de dependencias.
- [ ] Crear tests JUnit 5.
- [ ] Implementar o corregir contrato de inmutabilidad.
- [ ] Implementar o corregir fallback entorno/sistema.
- [ ] Normalizar nombres de fichero.
- [ ] Definir claves sensibles por defecto.
- [ ] Activar perfil de calidad en CI.
- [ ] Actualizar README.
- [ ] Corregir codificacion UTF-8 de recursos.
- [ ] Limpiar codigo auxiliar y scripts.
- [ ] Ejecutar validacion final.
- [ ] Preparar release/changelog.

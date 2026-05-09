# Resultado de aplicacion del plan de actuacion

Fecha: 2026-05-09
Rama: `codex-auditoria-remediacion-2026-05-09`

## Resumen

Se han aplicado las fases principales del plan de actuacion:

- build reproducible con Java 21;
- Maven Wrapper anadido;
- CI alineado con JDK 21;
- dependencias runtime actualizadas;
- tests JUnit 5 reales;
- contrato de API corregido con copias defensivas, fallback y normalizacion;
- claves sensibles por defecto;
- perfil de calidad con SpotBugs, Checkstyle y JaCoCo;
- README actualizado;
- recursos `.properties` normalizados en UTF-8;
- script PowerShell parametrizado y sin `Invoke-Expression`.

## Hallazgos cerrados

| Hallazgo | Estado | Evidencia |
| --- | --- | --- |
| A-001 | Cerrado | Workflow usa JDK 21 y `mvn verify` pasa con Java 21 |
| A-002 | Cerrado | 13 tests JUnit 5 anadidos |
| A-003 | Cerrado | `getProperties`, `getAllProperties` y `addProperties` usan copias defensivas |
| A-004 | Cerrado | `getProperty` aplica fallback fichero -> entorno -> propiedad de sistema |
| A-005 | Cerrado | Logback actualizado a 1.5.32 |
| A-006 | Cerrado | Jackson actualizado a 2.21.3 |
| A-007 | Cerrado | `mvn -Pquality verify` ejecuta tests, SpotBugs, Checkstyle y JaCoCo |
| A-008 | Cerrado | README reescrito con API actual |
| A-009 | Cerrado | `FinalDelProgramaHelper` ya no llama a `System.exit` |
| A-010 | Cerrado | Nombres con y sin `.properties` se normalizan |
| A-011 | Cerrado | Escrituras y getters sensibles sincronizados; valores defensivos |
| A-012 | Cerrado | Script parametrizado y sin ejecucion dinamica |
| A-013 | Cerrado | Recursos de ejemplo reescritos en UTF-8 |
| A-014 | Cerrado | Mensajes actualizados a `properties-helper` |
| A-015 | Cerrado | `@Slf4j` no usado eliminado del enum |
| A-016 | Cerrado | Mapa vacio permitido y cubierto por tests |

## Verificaciones ejecutadas

```text
mvn test
Resultado: BUILD SUCCESS, 13 tests, 0 fallos.

mvn verify
Resultado: BUILD SUCCESS, 13 tests, JAR generado.

mvn -Pquality verify
Resultado: BUILD SUCCESS, SpotBugs sin bugs, Checkstyle ejecutado, JaCoCo generado.

.\mvnw.cmd -version
Resultado: Maven Wrapper usa Apache Maven 3.9.15 con Java 21.0.9.

mvn dependency:tree "-Dincludes=ch.qos.logback:logback-core,com.fasterxml.jackson.core:jackson-core"
Resultado: logback-core 1.5.32 y jackson-core 2.21.3.
```

## Seguimiento recomendado

- Decidir si el cambio de `artifactId` a `properties-helper` es definitivo antes de publicar.
- Revisar los warnings de Checkstyle en una iteracion de formateo dedicada si se quiere convertirlos en errores.
- Ejecutar el workflow en GitHub Actions para validar OWASP Dependency Check en el entorno remoto.
- Preparar changelog y version de release cuando se confirme compatibilidad con consumidores.

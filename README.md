# Automated Multi-Framework COSMIC Function Point (AMF-CFP)

Herramienta para medir automáticamente el tamaño funcional de aplicaciones Java en
**COSMIC Function Points (CFP)**, mediante análisis estático de bytecode. Soporta
varios frameworks (**Spring Boot** y **Jakarta EE**).

Desarrollada como artefacto de la tesis de Maestría de Israel Parra Cámara.

## Cómo funciona

Para cada proyecto encontrado en la ruta indicada, la herramienta:

1. Escanea el proyecto y detecta el framework (Spring Boot / Jakarta EE).
2. Compila el proyecto con su herramienta de build (Maven o Gradle).
3. Prepara el artefacto generado (JAR/WAR).
4. Analiza el bytecode con ASM y detecta los movimientos de datos COSMIC
   (Entry, Exit, Read, Write) de cada proceso funcional.
5. Exporta un reporte JSON con el total de CFP por proyecto.

Las fronteras de cada framework se definen en archivos JSON dentro de `config/`.

## Estructura

```
.
├── pom.xml                  # Proyecto principal (analizador)
├── config/
│   ├── common/              # Fronteras comunes a todos los frameworks
│   └── frameworks/          # Fronteras específicas (springboot, jakartaee)
├── src/main/java/com/emir/analyzer/
│   ├── orchestrator/        # Punto de entrada (SpringJarAnalyzer)
│   ├── scanner/  detector/  build/  artifact/
│   ├── framework/           # Adaptadores y estrategias por framework
│   ├── engine/              # Motor de conteo COSMIC
│   ├── heuristics/          # Heurísticas de detección de persistencia
│   └── ...
└── cosmic-annotations/      # Librería con la anotación @CosmicLambda
```

`cosmic-annotations` es una librería aparte que el analizador usa como dependencia.
Contiene `@CosmicLambda`, una anotación para marcar movimientos de datos dentro de
expresiones lambda, que el análisis estático no puede detectar por sí solo.

## Requisitos

- Java 17 o superior
- Maven 3.9+

## Compilación

`cosmic-annotations` no está publicada en Maven Central, así que primero hay que
instalarla en el repositorio local de Maven:

```bash
mvn -f cosmic-annotations/pom.xml install
```

Después se compila el analizador:

```bash
mvn package
```

Se genera el JAR ejecutable `target/spring-analyzer-2.0-REFACTORED.jar`.

## Uso

Ejecuta la herramienta **desde la raíz del repositorio**, porque los archivos de
`config/` se cargan con rutas relativas al directorio de trabajo:

```bash
java -jar target/spring-analyzer-2.0-REFACTORED.jar -p /ruta/a/proyectos -o /ruta/salida
```

| Opción | Descripción |
|---|---|
| `-p`, `--project` | Ruta del proyecto o carpeta raíz con varios proyectos |
| `-o`, `--output`  | Directorio donde se guardan los reportes |
| `-c`, `--config`  | Ruta de un archivo de fronteras personalizado (opcional) |
| `-s`, `--scan`    | Paquetes a escanear (opcional) |

## Autor

Israel Parra Cámara

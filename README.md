# Automated Multi-Framework COSMIC Function Point (AMF-CFP)

Herramienta para medir automáticamente el tamaño funcional de aplicaciones Java en
**COSMIC Function Points (CFP)**, mediante análisis estático de bytecode. Soporta
varios frameworks:

| Framework | Cómo se analiza |
|---|---|
| **Spring Boot** | Adaptador Spring Boot (`@RestController`, Spring Data, Feign, RestTemplate, WebClient, etc.) |
| **Jakarta EE** | Adaptador Jakarta EE (JAX-RS, CDI, EJB, JPA) |
| **Quarkus** | Se detecta automáticamente y se analiza con el adaptador Jakarta EE (JAX-RS, CDI, Panache, REST Client). En la consola aparece como `Framework: Jakarta EE`. |

También analiza arquitecturas de **microservicios**: si la ruta contiene varios
proyectos (por ejemplo, un multi-módulo Maven), mide cada servicio por separado y
genera un total del sistema.

Desarrollada como artefacto de la tesis de Maestría de Israel Parra Cámara.

## Cómo funciona

Para cada proyecto encontrado en la ruta indicada, la herramienta:

1. Escanea el proyecto y detecta el framework (Spring Boot / Jakarta EE / Quarkus).
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

> **Recomendación: usa Java 17.** La herramienta compila el proyecto que vas a medir
> con el JDK activo. Los proyectos antiguos (por ejemplo, Java 11 con versiones viejas
> de Lombok) no compilan con Java 21 y sus servicios quedan fuera del conteo. Con
> Java 17 funcionan tanto los proyectos antiguos como los recientes (Spring Boot 3/4,
> Quarkus 3).
>
> En PowerShell puedes activar Java 17 solo para la ventana actual:
>
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
> $env:Path = "$env:JAVA_HOME\bin;$env:Path"
> ```

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

El reporte se guarda como `<nombre-del-proyecto>.json` en el directorio de salida,
con el total de CFP, los movimientos (Entry, Exit, Read, Write) y los CFP por clase
de cada servicio.

## Uso desde IntelliJ IDEA

1. **File → New → Project from Version Control** y pega la URL de este repositorio
   (o abre la carpeta ya clonada). IntelliJ detecta el `pom.xml` automáticamente.
2. En **File → Project Structure → Project → SDK**, selecciona un **JDK 17**.
3. Instala `cosmic-annotations` una sola vez. En la terminal de IntelliJ:

   ```bash
   mvn -f cosmic-annotations/pom.xml install
   ```

   Después, en la ventana **Maven**, pulsa **Reload All Maven Projects**.
4. Abre `src/main/java/com/emir/analyzer/orchestrator/SpringJarAnalyzer.java` y
   ejecútalo una vez con el botón ▶ (sin argumentos muestra la ayuda).
5. En **Run → Edit Configurations**, en la configuración `SpringJarAnalyzer`:
   - **Program arguments:** `-p "C:\ruta\al\proyecto" -o "C:\ruta\salida"`
   - **Working directory:** la carpeta raíz del repositorio (es el valor por
     defecto). Si apunta a otra carpeta, no encuentra `config/`.
6. Ejecuta de nuevo con ▶.

## Autor

Israel Parra Cámara

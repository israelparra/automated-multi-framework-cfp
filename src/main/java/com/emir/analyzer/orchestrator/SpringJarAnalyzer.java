package com.emir.analyzer.orchestrator;

import com.emir.analyzer.artifact.ArtifactPreparator;
import com.emir.analyzer.artifact.IArtifactPreparator;
import com.emir.analyzer.artifact.PreparedArtifact;
import com.emir.analyzer.build.BuildExecutor;
import com.emir.analyzer.build.BuildResult;
import com.emir.analyzer.build.IBuildExecutor;
import com.emir.analyzer.detector.FrameworkDetector;
import com.emir.analyzer.detector.IFrameworkDetector;
import com.emir.analyzer.engine.CosmicAnalyzer;
import com.emir.analyzer.engine.CosmicFunctionalModel;
import com.emir.analyzer.engine.ICosmicAnalyzer;
import com.emir.analyzer.framework.FrameworkContext;
import com.emir.analyzer.framework.FrameworkRegistry;
import com.emir.analyzer.framework.FrameworkType;
import com.emir.analyzer.scanner.IProjectScanner;
import com.emir.analyzer.scanner.ProjectMetadata;
import com.emir.analyzer.scanner.ProjectScanner;
import com.emir.analyzer.utils.JavaVersionDetector;
import org.apache.commons.cli.*;
import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;

/**
 * Entry point y orquestador principal del análisis COSMIC.
 * Subsistema: Orchestrator.
 *
 * El método analyzeAllProjects() es un espejo 1:1 del diagrama de secuencia.
 * Cada línea del loop corresponde a un par de mensajes (llamada + retorno).
 *
 * Dependencias (todas vía interfaces):
 * - IProjectScanner (msgs 1-2)
 * - IFrameworkDetector (msgs 3-4)
 * - FrameworkRegistry.resolveFrameworkContext (msgs 5-8)
 * - IBuildExecutor (msgs 9-10)
 * - IArtifactPreparator (msgs 11-12)
 * - ICosmicAnalyzer (msgs 13-19)
 *
 * NO contiene lógica de negocio COSMIC, compilación, detección de framework,
 * ni carga de configuración directamente.
 */
public class SpringJarAnalyzer {

    // Dependencias inyectadas (interfaces)
    private final IProjectScanner scanner;
    private final IFrameworkDetector detector;
    private final IBuildExecutor buildExecutor;
    private final IArtifactPreparator artifactPreparator;
    private final ICosmicAnalyzer cosmicAnalyzer;

    public SpringJarAnalyzer(String outputDirectory) {
        this.scanner = new ProjectScanner();
        this.detector = new FrameworkDetector();
        this.buildExecutor = new BuildExecutor();
        this.artifactPreparator = new ArtifactPreparator();
        this.cosmicAnalyzer = new CosmicAnalyzer(outputDirectory);
    }

    /**
     * Orquesta el análisis completo de todos los proyectos.
     * ESPEJO 1:1 DEL DIAGRAMA DE SECUENCIA.
     *
     * Msgs 1-2:  scanner.scanForProjects(rootDir) → List<ProjectMetadata>
     * Loop [para cada proyecto funcional]:
     *   Msgs 3-4:  detector.detectFunctionalFramework(project) → FrameworkType
     *   Msgs 5-8:  FrameworkRegistry.resolveFrameworkContext(fw) → FrameworkContext
     *   Msgs 9-10: buildExecutor.compile(path, buildType) → BuildResult
     *   Msgs 11-12: artifactPreparator.prepare(artifact, fw) → PreparedArtifact
     *   Msgs 13-19: cosmicAnalyzer.analyze(artifact, adapter, config) → CosmicFunctionalModel
     */
    public Map<String, Integer> analyzeAllProjects(String rootPath, String outputDirectory) {
        Map<String, Integer> cfpResults = new LinkedHashMap<>();
        Map<String, CosmicFunctionalModel> modelsMap = new LinkedHashMap<>();

        // ====================================================================
        // Msgs 1-2: scanForProjects(rootDir) → List<ProjectMetadata>
        // ====================================================================
        List<ProjectMetadata> projects = scanner.scanForProjects(new File(rootPath));

        if (projects.isEmpty()) {
            projects.add(new ProjectMetadata(
                    new File(rootPath).getName(), rootPath,
                    com.emir.analyzer.detector.FrameworkDetector.detectFramework(new File(rootPath))));
        }

        // ====================================================================
        // Loop [para cada proyecto funcional]
        // ====================================================================
        int processed = 0;
        for (ProjectMetadata project : projects) {

            // Msgs 3-4: detectFunctionalFramework(projectMetadata) → FrameworkType
            FrameworkType fw = detector.detectFunctionalFramework(project);
            if (fw == null || fw == FrameworkType.UNKNOWN) {
                continue;
            }

            System.out.println("\n-------------------------------------------------");
            System.out.println("▶️  PROCESANDO: " + project.getName());
            System.out.println("-------------------------------------------------");

            // Msgs 5-8: resolveFrameworkContext(frameworkType) → FrameworkContext
            //   (internamente: msgs 6-7 loadForFramework → ConfiguracionFronteras)
            FrameworkContext ctx = FrameworkRegistry.resolveFrameworkContext(fw);

            // Msgs 9-10: compile(projectDir, buildType) → BuildResult
            BuildResult build = buildExecutor.compile(project.getPath(), project.getBuildType());
            if (!build.isSuccess()) {
                System.err.println("❌ Falló la compilación de: " + project.getName());
                cfpResults.put(project.getName(), 0);
                continue;
            }

            // Msgs 11-12: prepare(jarFile, frameworkType) → PreparedArtifact
            PreparedArtifact artifact = artifactPreparator.prepare(build.getArtifactFile(), fw);

            // Msgs 13-19: analyze(preparedArtifact, adapter, config) → CosmicFunctionalModel
            //   (internamente: msgs 14-15 classifyPersistence, msgs 16-17 getLambdaMovements,
            //    msg 18 exportReport)
            CosmicFunctionalModel model = cosmicAnalyzer.analyze(artifact, ctx.adapter(), ctx.config());

            if (model != null) {
                modelsMap.put(project.getName(), model);
                cfpResults.put(project.getName(), model.calculateTotalCFP());
            } else {
                cfpResults.put(project.getName(), 0);
            }

            processed++;
        }

        System.out.println("\n📦 Total de proyectos funcionales analizados: " + processed);

        // Export único consolidado
        String rootProjectName = new File(rootPath).getName();
        CosmicFunctionalModel.exportConsolidatedReport(rootProjectName, modelsMap, outputDirectory);

        return cfpResults;
    }

    // ========================================================================
    // MAIN — Entry point CLI (parseo de argumentos y pasos preliminares)
    // ========================================================================
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("p", "project", true, "Ruta del proyecto o carpeta raíz");
        options.addOption("o", "output", true, "Directorio de reportes");
        options.addOption("c", "config", true, "Ruta de fronteras.json (opcional)");
        options.addOption("s", "scan", true, "Paquetes a escanear (opcional)");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (!cmd.hasOption("p") || !cmd.hasOption("o")) {
                printUsage(options); return;
            }

            String projectPath = cmd.getOptionValue("p");
            String outputDirectory = cmd.getOptionValue("o");

            System.out.println("╔═══════════════════════════════════════════════════════════╗");
            System.out.println("║  ANALIZADOR COSMIC FUNCTION POINTS - MULTI-FRAMEWORK      ║");
            System.out.println("║           Spring Boot + Jakarta EE Support                ║");
            System.out.println("╚═══════════════════════════════════════════════════════════╝");

            // Paso preliminar: Compilar proyecto multi-módulo desde raíz
            compileRootIfMultiModule(projectPath);

            // Paso preliminar 2: Compilar bibliotecas
            compileLibraryProjects(projectPath);

            // Análisis principal (espejo del diagrama de secuencia)
            SpringJarAnalyzer orchestrator = new SpringJarAnalyzer(outputDirectory);
            Map<String, Integer> cfpResults = orchestrator.analyzeAllProjects(projectPath, outputDirectory);

            // Resumen final consolidado
            if (cfpResults.size() > 1) {
                System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
                System.out.println("║           RESUMEN COSMIC - ARQUITECTURA COMPLETA          ║");
                System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
                int totalCFP = 0;
                for (Map.Entry<String, Integer> entry : cfpResults.entrySet()) {
                    System.out.println(String.format("   %-45s %6d CFP", entry.getKey() + ":", entry.getValue()));
                    totalCFP += entry.getValue();
                }
                System.out.println("   " + "─".repeat(55));
                System.out.println(String.format("   %-45s %6d CFP", "TOTAL SISTEMA:", totalCFP));
            }

            System.out.println("\n✅ PROCESO COMPLETADO.");

        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage(options);
        }
    }

    private static void compileRootIfMultiModule(String projectPath) {
        File rootPom = new File(projectPath, "pom.xml");
        if (!rootPom.exists()) return;

        try {
            String pomContent = new String(Files.readAllBytes(rootPom.toPath()));
            if (!pomContent.contains("<modules>") && !pomContent.contains("<module>")) return;

            System.out.println("✅ Detectado proyecto multi-módulo Maven. Compilando desde raíz...");
            int currentJavaVersion = JavaVersionDetector.getCurrentJavaVersion();
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            List<String> command = new ArrayList<>();
            command.add(isWindows ? "mvn.cmd" : "mvn");
            command.add("clean"); command.add("install"); command.add("-DskipTests");
            command.add("-Dmaven.javadoc.skip=true");
            if (currentJavaVersion < 21) {
                command.add("-Dmaven.compiler.source=" + currentJavaVersion);
                command.add("-Dmaven.compiler.target=" + currentJavaVersion);
                command.add("-Dmaven.compiler.release=" + currentJavaVersion);
            }
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("BUILD SUCCESS") || line.contains("BUILD FAILURE") ||
                        line.contains("Reactor Summary")) {
                    System.out.println("[Maven Root] " + line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) System.out.println("✅ Compilación raíz exitosa.\n");
            else System.err.println("⚠️ Compilación raíz falló. Continuando...\n");
        } catch (Exception e) {
            System.err.println("⚠️ Error al compilar raíz: " + e.getMessage());
        }
    }

    private static void compileLibraryProjects(String projectPath) {
        // The build executor handles pre-compiled artifacts
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar cfp-analyzer.jar", options);
        System.out.println("\nEjemplos:");
        System.out.println("  java -jar cfp-analyzer.jar -p /ruta/proyecto -o /ruta/salida");
    }
}

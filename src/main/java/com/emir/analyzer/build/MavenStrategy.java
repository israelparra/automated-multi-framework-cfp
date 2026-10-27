package com.emir.analyzer.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MavenStrategy implements BuildToolStrategy {

    @Override
    public boolean isApplicable(File projectDir) {
        return new File(projectDir, "pom.xml").exists();
    }

    @Override
    public boolean build(File projectDir) {
        String mvnCommand = System.getProperty("os.name").toLowerCase().contains("windows") ? "mvn.cmd" : "mvn";

        int requiredJavaVersion = detectRequiredJavaVersion(projectDir);

        List<String> command = new ArrayList<>();
        command.add(mvnCommand);
        command.add("clean");
        command.add("package");
        command.add("-DskipTests");
        command.add("-Djib.skip");
        command.add("-Dtestcontainers.skip=true");
        command.add("-Dcodegen.skip=true");

        if (requiredJavaVersion >= 21) {
            int currentJavaVersion = getCurrentJavaVersion();
            if (currentJavaVersion < 21) {
                command.add("-Dmaven.compiler.source=" + currentJavaVersion);
                command.add("-Dmaven.compiler.target=" + currentJavaVersion);
                command.add("-Dmaven.compiler.release=" + currentJavaVersion);
            }
        }

        return runCommand(projectDir, command.toArray(new String[0]));
    }

    @Override
    public File findOutputJar(File projectDir) {
        File targetDir = new File(projectDir, "target");
        if (!targetDir.exists()) return null;

        File[] files = targetDir.listFiles((dir, name) ->
                (name.endsWith(".jar") || name.endsWith(".war")) &&
                        !name.contains("original-"));

        return (files != null && files.length > 0) ? files[0] : null;
    }

    @Override
    public String getName() {
        return "Maven";
    }

    /**
     * ✅ NUEVO: Detecta la versión de Java requerida por el proyecto.
     */
    private int detectRequiredJavaVersion(File projectDir) {
        File pomFile = new File(projectDir, "pom.xml");

        if (!pomFile.exists()) {
            return 0;
        }

        try {
            String pomContent = new String(java.nio.file.Files.readAllBytes(pomFile.toPath()));

            // Buscar <maven.compiler.target>21</maven.compiler.target>
            if (pomContent.contains("<maven.compiler.target>21</maven.compiler.target>") ||
                    pomContent.contains("<maven.compiler.source>21</maven.compiler.source>") ||
                    pomContent.contains("<maven.compiler.release>21</maven.compiler.release>")) {
                return 21;
            }

            if (pomContent.contains("<maven.compiler.target>17</maven.compiler.target>") ||
                    pomContent.contains("<maven.compiler.source>17</maven.compiler.source>")) {
                return 17;
            }

            if (pomContent.contains("<maven.compiler.target>11</maven.compiler.target>") ||
                    pomContent.contains("<maven.compiler.source>11</maven.compiler.source>")) {
                return 11;
            }

            // Buscar en <properties> con regex más flexible
            if (pomContent.matches("(?s).*<java\\.version>21</java\\.version>.*")) {
                return 21;
            }

            if (pomContent.matches("(?s).*<java\\.version>17</java\\.version>.*")) {
                return 17;
            }

        } catch (Exception e) {
            // Ignorar errores
        }

        return 0;
    }

    /**
     * ✅ NUEVO: Obtiene la versión de Java actual.
     */
    private int getCurrentJavaVersion() {
        String version = System.getProperty("java.version");

        // Formato: "17.0.1" o "1.8.0_292"
        if (version.startsWith("1.")) {
            // Java 8 o anterior: "1.8.0_292"
            return Integer.parseInt(version.split("\\.")[1]);
        } else {
            // Java 9+: "17.0.1"
            return Integer.parseInt(version.split("\\.")[0]);
        }
    }

    private boolean runCommand(File dir, String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(dir);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("BUILD SUCCESS") || line.contains("BUILD FAILURE") ||
                            line.contains("ERROR") || line.contains("WARNING")) {
                        System.out.println("   [Maven] " + line.trim());
                    }
                }
            }
            return process.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
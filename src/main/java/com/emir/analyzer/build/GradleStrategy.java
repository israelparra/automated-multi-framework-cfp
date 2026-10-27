package com.emir.analyzer.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GradleStrategy implements BuildToolStrategy {

    @Override
    public boolean isApplicable(File projectDir) {
        return new File(projectDir, "build.gradle").exists() ||
                new File(projectDir, "build.gradle.kts").exists();
    }

    @Override
    public boolean build(File projectDir) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("windows");

        String wrapperName = isWindows ? "gradlew.bat" : "gradlew";
        String commandToRun = null;

        // 1. Buscar Wrapper en el directorio del proyecto
        File wrapper = new File(projectDir, wrapperName);

        // 2. Si no está, buscar Wrapper en el directorio PADRE
        File parentWrapper = new File(projectDir.getParentFile(), wrapperName);

        // 3. Si no está, buscar en el ABUELO (común en repositorios multi-nivel)
        File grandParentWrapper = new File(projectDir.getParentFile().getParentFile(), wrapperName);

        if (wrapper.exists()) {
            commandToRun = wrapper.getAbsolutePath();
        } else if (parentWrapper.exists()) {
            commandToRun = parentWrapper.getAbsolutePath();
        } else if (grandParentWrapper.exists()) {
            commandToRun = grandParentWrapper.getAbsolutePath();
        } else {
            commandToRun = "gradle";
        }

        List<String> commandList = new ArrayList<>();

        if (isWindows && commandToRun.endsWith(".bat")) {
            commandList.add("cmd");
            commandList.add("/c");
            commandList.add(commandToRun);
        } else {
            commandList.add(commandToRun);
        }

// Argumentos optimizados para solo compilar
        commandList.add("clean");
        commandList.add("assemble"); // 'assemble' es más seguro que 'build' (evita tests)
        commandList.add("-x");
        commandList.add("test");
        commandList.add("-x");
        commandList.add("check");    // Salta PMD, Checkstyle, Spotless, etc.
        commandList.add("--no-daemon");

        return runCommand(projectDir, commandList);
    }

    @Override
    public File findOutputJar(File projectDir) {
        File libsDir = new File(projectDir, "build/libs");
        if (!libsDir.exists()) return null;

        // ACEPTAR .jar Y .war
        File[] files = libsDir.listFiles((dir, name) -> {
            boolean isJarOrWar = name.endsWith(".jar") || name.endsWith(".war");
            boolean isNotPlain = !name.endsWith("-plain.jar") && !name.endsWith("-plain.war");
            return isJarOrWar && isNotPlain;
        });

        return (files != null && files.length > 0) ? files[0] : null;
    }

    @Override
    public String getName() {
        return "Gradle";
    }

    private boolean runCommand(File dir, List<String> command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(dir);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("BUILD SUCCESSFUL") || line.contains("BUILD FAILED") ||
                            line.contains("error:") || line.toUpperCase().startsWith("FAILURE") ||
                            line.contains("Task :") && line.contains("FAILED")) {
                        System.out.println("   [Gradle] " + line.trim());
                    }
                }
            }
            return process.waitFor() == 0;
        } catch (Exception e) {
            System.err.println("❌ Error ejecutando Gradle: " + e.getMessage());
            return false;
        }
    }
}
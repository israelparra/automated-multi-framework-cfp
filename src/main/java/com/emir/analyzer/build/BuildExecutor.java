package com.emir.analyzer.build;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Fachada que implementa IBuildExecutor.
 * Subsistema: Build & Compilation.
 *
 * Selecciona la estrategia correcta (Maven/Gradle) y ejecuta la compilación.
 */
public class BuildExecutor implements IBuildExecutor {

    private final List<BuildToolStrategy> strategies;

    public BuildExecutor() {
        this.strategies = Arrays.asList(new MavenStrategy(), new GradleStrategy());
    }

    @Override
    public BuildResult compile(String projectDir, String buildType) {
        File dir = new File(projectDir);

        // Verificar si ya existe un artefacto compilado (compilación desde raíz)
        File precompiledArtifact = findPrecompiledArtifact(dir);
        if (precompiledArtifact != null) {
            System.out.println("✅ Artefacto ya compilado encontrado: " + precompiledArtifact.getName());
            System.out.println("   (Reutilizando compilación desde raíz)");
            return new BuildResult(true, precompiledArtifact);
        }

        // Seleccionar estrategia y compilar
        for (BuildToolStrategy strategy : strategies) {
            if (strategy.isApplicable(dir)) {
                System.out.println("🔨 Detectado sistema de construcción: " + strategy.getName());
                boolean success = strategy.build(dir);

                if (success) {
                    File artifact = strategy.findOutputJar(dir);
                    return new BuildResult(artifact != null, artifact);
                } else {
                    return new BuildResult(false, null);
                }
            }
        }

        System.err.println("❌ No se detectó un sistema de construcción soportado.");
        return new BuildResult(false, null);
    }

    private File findPrecompiledArtifact(File projectDir) {
        File targetDir = new File(projectDir, "target");
        if (targetDir.exists()) {
            File[] artifacts = targetDir.listFiles((dir, name) ->
                    name.endsWith(".war") || name.endsWith(".jar"));
            if (artifacts != null && artifacts.length > 0) {
                return artifacts[0];
            }
        }

        // Check Gradle
        File libsDir = new File(projectDir, "build/libs");
        if (libsDir.exists()) {
            File[] artifacts = libsDir.listFiles((dir, name) ->
                    (name.endsWith(".jar") || name.endsWith(".war")) &&
                            !name.endsWith("-plain.jar") && !name.endsWith("-plain.war"));
            if (artifacts != null && artifacts.length > 0) {
                return artifacts[0];
            }
        }

        return null;
    }
}

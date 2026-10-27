package com.emir.analyzer.build;

/**
 * Interfaz del subsistema Build & Compilation.
 *
 * Corresponde a los mensajes 9-10 del diagrama de secuencia:
 * compile(projectDir, buildType) → BuildResult
 */
public interface IBuildExecutor {

    /**
     * Compila un proyecto Java usando Maven o Gradle.
     *
     * @param projectDir Ruta del directorio del proyecto
     * @param buildType Tipo de build system ("MAVEN" o "GRADLE")
     * @return BuildResult con indicador de éxito y referencia al artefacto generado
     */
    BuildResult compile(String projectDir, String buildType);
}

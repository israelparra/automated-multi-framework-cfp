package com.emir.analyzer.scanner;

import com.emir.analyzer.framework.FrameworkType;

/**
 * DTO que representa la información de un proyecto detectado.
 * Reemplaza MicroserviceInfo. Resultado del escaneo de proyectos.
 *
 * Campos: name, path, buildType (inferido de FrameworkType para compatibilidad).
 *
 * DECISIÓN: Se mantiene FrameworkType en lugar de BuildType separado porque
 * en el código actual MicroserviceInfo ya incluye FrameworkType y el buildType
 * se infiere de la presencia de pom.xml/build.gradle en ProjectScanner.
 * Se agrega campo buildType como String ("MAVEN"/"GRADLE") para cumplir la especificación.
 */
public class ProjectMetadata {

    private final String name;
    private final String path;
    private final String buildType;
    private final FrameworkType framework;

    public ProjectMetadata(String name, String path, String buildType, FrameworkType framework) {
        this.name = name;
        this.path = path;
        this.buildType = buildType;
        this.framework = framework;
    }

    /**
     * Constructor de compatibilidad (sin buildType explícito).
     */
    public ProjectMetadata(String name, String path, FrameworkType framework) {
        this.name = name;
        this.path = path;
        this.buildType = inferBuildType(path);
        this.framework = framework;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getBuildType() {
        return buildType;
    }

    public FrameworkType getFramework() {
        return framework;
    }

    private static String inferBuildType(String path) {
        java.io.File dir = new java.io.File(path);
        if (new java.io.File(dir, "pom.xml").exists()) return "MAVEN";
        if (new java.io.File(dir, "build.gradle").exists()) return "GRADLE";
        if (new java.io.File(dir, "build.gradle.kts").exists()) return "GRADLE";
        return "MAVEN";
    }

    @Override
    public String toString() {
        return "ProjectMetadata{name='" + name + "', framework=" + framework.getDisplayName() +
                ", buildType=" + buildType + ", path='" + path + "'}";
    }
}

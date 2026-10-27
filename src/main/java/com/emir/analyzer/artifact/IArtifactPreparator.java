package com.emir.analyzer.artifact;

import com.emir.analyzer.framework.FrameworkType;

import java.io.File;

/**
 * Interfaz del subsistema Artifact Preparation.
 *
 * Corresponde a los mensajes 11-12 del diagrama de secuencia:
 * prepare(jarFile, frameworkType) → PreparedArtifact
 */
public interface IArtifactPreparator {

    /**
     * Extrae un JAR/WAR, crea URLClassLoader aislado y carga clases en memoria.
     *
     * @param jarFile Archivo JAR/WAR compilado
     * @param frameworkType Tipo de framework (para estructura interna del JAR)
     * @return PreparedArtifact con classLoader, classNames y loadedClasses
     */
    PreparedArtifact prepare(File jarFile, com.emir.analyzer.framework.FrameworkType frameworkType);
}

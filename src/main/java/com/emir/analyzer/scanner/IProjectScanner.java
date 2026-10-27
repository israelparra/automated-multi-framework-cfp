package com.emir.analyzer.scanner;

import java.io.File;
import java.util.List;

/**
 * Interfaz del subsistema Project Scanner.
 *
 * Corresponde a los mensajes 1-2 del diagrama de secuencia:
 * scanForProjects(rootDir) → List<ProjectMetadata>
 */
public interface IProjectScanner {

    /**
     * Escanea recursivamente un directorio raíz buscando proyectos Maven/Gradle.
     *
     * @param rootDir Directorio raíz a escanear
     * @return Lista de proyectos encontrados con sus metadatos
     */
    List<ProjectMetadata> scanForProjects(File rootDir);
}

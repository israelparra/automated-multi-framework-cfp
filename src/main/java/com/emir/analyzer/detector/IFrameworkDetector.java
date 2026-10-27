package com.emir.analyzer.detector;

import com.emir.analyzer.framework.FrameworkType;
import com.emir.analyzer.scanner.ProjectMetadata;

/**
 * Interfaz del subsistema Framework Detector.
 *
 * Corresponde a los mensajes 3-4 del diagrama de secuencia:
 * detectFunctionalFramework(projectMetadata) → FrameworkType
 *
 * Unifica detección de framework + validación funcional.
 */
public interface IFrameworkDetector {

    /**
     * Detecta el framework de un proyecto y verifica si es funcional.
     *
     * @param metadata Metadatos del proyecto (nombre, ruta, buildType)
     * @return FrameworkType si el proyecto es funcional, null si es infraestructura o desconocido
     */
    FrameworkType detectFunctionalFramework(ProjectMetadata metadata);
}

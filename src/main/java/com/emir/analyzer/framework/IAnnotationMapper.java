package com.emir.analyzer.framework;

import java.util.Set;

/**
 * Interface para mapeo de anotaciones entre diferentes frameworks
 */
public interface IAnnotationMapper {

    /**
     * Obtiene el nombre de la anotación de controlador REST
     */
    String getRestControllerAnnotation();

    /**
     * Obtiene los nombres de todas las anotaciones de mapeo HTTP
     */
    Set<String> getHttpMappingAnnotations();

    /**
     * Obtiene el nombre de la anotación de entidad
     */
    Set<String> getEntityAnnotations();

    /**
     * Obtiene el nombre de la anotación de repositorio
     */
    Set<String> getRepositoryAnnotations();

    /**
     * Obtiene el nombre de la anotación de aplicación principal
     */
    String getMainApplicationAnnotation();

    /**
     * Obtiene el nombre de la anotación de inyección de dependencias
     */
    String getInjectAnnotation();

    /**
     * Obtiene el nombre de la anotación transaccional
     */
    String getTransactionalAnnotation();
}
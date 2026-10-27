package com.emir.analyzer.framework;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

/**
 * Interfaz principal del Strategy Pattern para manejo de frameworks.
 * Centraliza TODA la lógica dependiente del framework en un solo contrato.
 *
 * Cada framework (Spring Boot, Jakarta EE, Micronaut, etc.) debe implementar esta interfaz.
 */
public interface IFrameworkStrategy {

    // ========== IDENTIFICACIÓN ==========

    /**
     * Obtiene el tipo de framework que maneja esta estrategia.
     */
    FrameworkType getFrameworkType();

    /**
     * Nombre legible del framework (para logs).
     */
    String getDisplayName();

    // ========== DETECCIÓN DE PROYECTOS ==========

    /**
     * Determina si un proyecto es funcional (microservicio de negocio)
     * o infraestructura (gateway, service registry, etc.).
     *
     * @param projectPath Ruta del proyecto
     * @param projectName Nombre del proyecto
     * @return true si es funcional, false si es infraestructura
     */
    boolean isFunctional(File projectPath, String projectName);

    /**
     * Detecta si un proyecto es de infraestructura basándose en:
     * - Dependencias del build file
     * - Anotaciones del código fuente
     * - Nombre del proyecto
     *
     * @param buildFilePath Ruta al pom.xml o build.gradle
     * @param srcDir Directorio src/main/java
     * @return true si es infraestructura
     */
    boolean isInfrastructure(Path buildFilePath, File srcDir);

    /**
     * Obtiene las dependencias de infraestructura específicas del framework.
     * Ejemplo Spring: ["spring-cloud-starter-gateway", "spring-cloud-netflix-eureka-server"]
     * Ejemplo Jakarta: [] (generalmente no tiene infraestructura específica)
     */
    String[] getInfrastructureDependencies();

    /**
     * Obtiene las anotaciones de infraestructura específicas del framework.
     * Ejemplo Spring: ["@EnableEurekaServer", "@EnableConfigServer"]
     */
    String[] getInfrastructureAnnotations();

    // ========== ADAPTADORES ==========

    /**
     * Obtiene el adapter completo para análisis de componentes.
     * Esto mantiene compatibilidad con el sistema actual.
     */
    IFrameworkAdapter getAdapter();

    /**
     * Obtiene el mapper de anotaciones.
     */
    IAnnotationMapper getAnnotationMapper();

    /**
     * Obtiene el scanner de componentes.
     */
    IComponentScanner getComponentScanner();

}
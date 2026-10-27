package com.emir.analyzer.framework;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Interface principal que abstrae el comportamiento específico de cada framework.
 * Cada framework (Spring Boot, Jakarta) debe implementar esta interfaz.
 */
public interface IFrameworkAdapter {

    /**
     * Obtiene el tipo de framework
     */
    FrameworkType getFrameworkType();

    /**
     * Obtiene el mapper de anotaciones específico del framework
     */
    IAnnotationMapper getAnnotationMapper();

    /**
     * Obtiene el scanner de componentes específico del framework
     */
    IComponentScanner getComponentScanner();

    /**
     * Verifica si una clase es un controlador REST
     */
    boolean isRestController(Class<?> clazz);

    /**
     * Verifica si una clase es una entidad
     */
    boolean isEntity(Class<?> clazz);

    /**
     * Verifica si una clase/interfaz es un repositorio de datos
     */
    boolean isDataRepository(Class<?> clazz);

    /**
     * Verifica si un método tiene anotación de endpoint REST
     */
    boolean hasRestEndpointAnnotation(Method method);

    /**
     * Obtiene el tipo de operación HTTP de un método (GET, POST, PUT, DELETE)
     */
    String getHttpMethodType(Method method);

    /**
     * Verifica si una clase es la clase principal de la aplicación
     */
    boolean isMainApplicationClass(Class<?> clazz);

    /**
     * Indica si este framework requiere deduplicación de movimientos COSMIC.
     */
    boolean requiresMovementDeduplication();



}
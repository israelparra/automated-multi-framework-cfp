package com.emir.analyzer.framework;

import java.util.Set;

/**
 * Interface para escaneo de componentes específicos del framework
 */
public interface IComponentScanner {

    /**
     * Auto-descubre los paquetes a escanear desde la clase principal
     */
    Set<String> autoDiscoverPackages(Class<?> mainAppClass);

    /**
     * Extrae los paquetes base desde las anotaciones de la clase
     */
    Set<String> extractBasePackages(Class<?> clazz);

    /**
     * Obtiene el paquete por defecto basado en la clase principal
     */
    String getDefaultPackage(Class<?> mainAppClass);
}
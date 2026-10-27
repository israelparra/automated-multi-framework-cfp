package com.emir.analyzer.utils;

/**
 * Utilidad para detectar la versión de Java en ejecución.
 *
 * Extraído de SpringJarAnalyzer para seguir el principio de responsabilidad única.
 *
 * @author Sistema de Análisis COSMIC
 */
public class JavaVersionDetector {

    /**
     * Obtiene la versión de Java actual en ejecución.
     *
     * Soporta tanto el formato antiguo (1.8.0_292) como el nuevo (17.0.1).
     *
     * @return Número de versión (8, 11, 17, 21, etc.)
     */
    public static int getCurrentJavaVersion() {
        String version = System.getProperty("java.version");

        // Formato antiguo: "1.8.0_292" → Java 8
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.split("\\.")[1]);
        }
        // Formato nuevo: "17.0.1" → Java 17
        else {
            return Integer.parseInt(version.split("\\.")[0]);
        }
    }
}
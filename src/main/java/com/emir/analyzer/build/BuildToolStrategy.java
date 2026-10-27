package com.emir.analyzer.build;

import java.io.File;

public interface BuildToolStrategy {
    /**
     * Verifica si esta estrategia aplica al proyecto (ej. ¿tiene pom.xml?)
     */
    boolean isApplicable(File projectDir);

    /**
     * Ejecuta el comando de compilación.
     */
    boolean build(File projectDir);

    /**
     * Encuentra el JAR generado en la carpeta de salida correspondiente.
     */
    File findOutputJar(File projectDir);

    /**
     * Nombre de la herramienta (para logs).
     */
    String getName();
}
package com.emir.analyzer.artifact;

import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

/**
 * DTO que empaqueta clases cargadas en memoria, listas para análisis.
 * Subsistema: Artifact Preparation.
 *
 * Corresponde al retorno del mensaje 12 del diagrama de secuencia.
 */
public class PreparedArtifact {

    private final URLClassLoader classLoader;
    private final Set<String> allClassNames;
    private final List<Class<?>> loadedClasses;

    public PreparedArtifact(URLClassLoader classLoader, Set<String> allClassNames, List<Class<?>> loadedClasses) {
        this.classLoader = classLoader;
        this.allClassNames = allClassNames;
        this.loadedClasses = loadedClasses;
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    public Set<String> getAllClassNames() {
        return allClassNames;
    }

    public List<Class<?>> getLoadedClasses() {
        return loadedClasses;
    }
}

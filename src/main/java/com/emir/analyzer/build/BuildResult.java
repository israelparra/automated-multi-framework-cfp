package com.emir.analyzer.build;

import java.io.File;

/**
 * DTO que empaqueta el resultado de una compilación.
 * Subsistema: Build & Compilation.
 *
 * Corresponde al retorno del mensaje 10 del diagrama de secuencia.
 */
public class BuildResult {

    private final boolean success;
    private final File artifactFile;

    public BuildResult(boolean success, File artifactFile) {
        this.success = success;
        this.artifactFile = artifactFile;
    }

    public boolean isSuccess() {
        return success;
    }

    public File getArtifactFile() {
        return artifactFile;
    }

    @Override
    public String toString() {
        return "BuildResult{success=" + success +
                ", artifactFile=" + (artifactFile != null ? artifactFile.getName() : "null") + "}";
    }
}

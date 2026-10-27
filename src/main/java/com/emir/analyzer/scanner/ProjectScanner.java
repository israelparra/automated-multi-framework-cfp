package com.emir.analyzer.scanner;

import com.emir.analyzer.framework.FrameworkType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de IProjectScanner.
 * Subsistema: Project Scanner.
 *
 * Responsabilidad: Escaneo recursivo de directorios para descubrir proyectos Java.
 * NO detecta frameworks (eso lo hace Framework Detector).
 * NO filtra funcionalidad.
 */
public class ProjectScanner implements IProjectScanner {

    @Override
    public List<ProjectMetadata> scanForProjects(File rootDir) {
        List<ProjectMetadata> projects = new ArrayList<>();

        System.out.println("🔍 [AUTO-DISCOVERY] Escaneando arquitectura en: " + rootDir.getAbsolutePath());

        findProjects(rootDir, projects);

        return projects;
    }

    private void findProjects(File directory, List<ProjectMetadata> projects) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }

        File pomFile = new File(directory, "pom.xml");
        File gradleFile = new File(directory, "build.gradle");
        File gradleKtsFile = new File(directory, "build.gradle.kts");

        if (pomFile.exists() || gradleFile.exists() || gradleKtsFile.exists()) {
            String projectName = directory.getName();
            String buildType = pomFile.exists() ? "MAVEN" : "GRADLE";

            ProjectMetadata info = new ProjectMetadata(
                    projectName,
                    directory.getAbsolutePath(),
                    buildType,
                    FrameworkType.UNKNOWN
            );
            projects.add(info);
            System.out.println("   [Encontrado] " + projectName + " (" + buildType + ")");
        }

        File[] subdirs = directory.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                String name = subdir.getName();
                if (!name.equals("target") && !name.equals("build") &&
                        !name.equals(".git") && !name.equals("node_modules") &&
                        !name.startsWith(".")) {
                    findProjects(subdir, projects);
                }
            }
        }
    }
}

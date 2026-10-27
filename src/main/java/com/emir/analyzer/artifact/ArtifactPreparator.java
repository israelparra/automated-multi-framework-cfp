package com.emir.analyzer.artifact;

import com.emir.analyzer.framework.FrameworkType;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Implementación de IArtifactPreparator.
 * Subsistema: Artifact Preparation.
 *
 * Extraída de SourceCodeAnalyzer.setupClassLoader().
 * Lógica de extracción de JAR/WAR y carga de clases en memoria.
 * La lógica funcional es IDÉNTICA a la original.
 */
public class ArtifactPreparator implements IArtifactPreparator {

    @Override
    public PreparedArtifact prepare(File jarFile, FrameworkType frameworkType) {
        try {
            File tempDir = Files.createTempDirectory("analyzer-temp").toFile();
            Set<String> allClassNames = new HashSet<>();

            URLClassLoader classLoader = setupClassLoader(jarFile, tempDir, allClassNames);
            Thread.currentThread().setContextClassLoader(classLoader);

            // Cargar todas las clases
            List<Class<?>> loadedClasses = new ArrayList<>();
            for (String className : allClassNames) {
                try {
                    String javaName = className.replace('/', '.');
                    Class<?> clazz = classLoader.loadClass(javaName);
                    loadedClasses.add(clazz);
                } catch (Throwable e) {
                    // Ignorar errores de carga
                }
            }

            return new PreparedArtifact(classLoader, allClassNames, loadedClasses);

        } catch (IOException e) {
            throw new RuntimeException("Error preparando artefacto: " + jarFile.getName(), e);
        }
    }

    /**
     * Extrae el contenido de un JAR o WAR a un directorio temporal.
     * LÓGICA IDÉNTICA a SourceCodeAnalyzer.setupClassLoader() original.
     */
    private URLClassLoader setupClassLoader(File jarFile, File tempDir, Set<String> allClassNamesOut) throws IOException {
        List<URL> urls = new ArrayList<>();

        File classesDir = new File(tempDir, "classes");
        classesDir.mkdirs();
        urls.add(classesDir.toURI().toURL());

        File libDir = new File(tempDir, "lib");
        libDir.mkdirs();

        boolean isSpringBootJar = false;
        boolean isQuarkusJar = false;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("BOOT-INF/") || name.startsWith("WEB-INF/")) {
                    isSpringBootJar = true;
                    break;
                }
                if (name.endsWith(".class") && !name.contains("META-INF") &&
                        !name.startsWith("org/") && !name.startsWith("javax/") &&
                        !name.startsWith("jakarta/") && !name.startsWith("java/")) {
                    isQuarkusJar = true;
                }
            }
        }


        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                File outFile = null;

                if (isSpringBootJar) {
                    boolean isBootClass = name.startsWith("BOOT-INF/classes/");
                    boolean isWebClass = name.startsWith("WEB-INF/classes/");
                    boolean isBootLib = name.startsWith("BOOT-INF/lib/");
                    boolean isWebLib = name.startsWith("WEB-INF/lib/");

                    if (isBootClass || isWebClass) {
                        int prefixLen = isBootClass ? "BOOT-INF/classes/".length() : "WEB-INF/classes/".length();
                        outFile = new File(classesDir, name.substring(prefixLen));
                        if (name.endsWith(".class")) {
                            allClassNamesOut.add(name.substring(prefixLen).replace(".class", ""));
                        }
                    } else if (isBootLib || isWebLib) {
                        outFile = new File(libDir, new File(name).getName());
                        if (!entry.isDirectory() && name.endsWith(".jar")) {
                            outFile.getParentFile().mkdirs();
                            try (InputStream is = jar.getInputStream(entry);
                                 OutputStream os = new FileOutputStream(outFile)) {
                                is.transferTo(os);
                            }
                            urls.add(outFile.toURI().toURL());
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    if (name.startsWith("META-INF/") ||
                            name.startsWith("org/eclipse/") ||
                            name.startsWith("org/jboss/") ||
                            name.startsWith("io/quarkus/") ||
                            name.startsWith("io/smallrye/") ||
                            name.startsWith("io/vertx/") ||
                            name.startsWith("jakarta/") ||
                            name.startsWith("javax/") ||
                            name.startsWith("java/")) {
                        continue;
                    }
                    if (name.endsWith(".class")) {
                        outFile = new File(classesDir, name);
                        allClassNamesOut.add(name.replace(".class", ""));
                    } else {
                        continue;
                    }
                }

                if (!entry.isDirectory() && outFile != null) {
                    outFile.getParentFile().mkdirs();
                    try (InputStream is = jar.getInputStream(entry);
                         OutputStream os = new FileOutputStream(outFile)) {
                        is.transferTo(os);
                    }
                }
            }
        }

        System.out.println("   \u2705 Artefacto preparado. Total clases extraídas: " + allClassNamesOut.size());
        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }
}

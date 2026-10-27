package com.emir.analyzer.detector;

import com.emir.analyzer.framework.FrameworkRegistry;
import com.emir.analyzer.framework.FrameworkType;
import com.emir.analyzer.framework.IFrameworkStrategy;
import com.emir.analyzer.scanner.ProjectMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementación de IFrameworkDetector.
 * Subsistema: Framework Detector.
 *
 * Unifica detectFramework() + isFunctional() en un solo método.
 * Dependencias: IFrameworkStrategy (del FAL, para validar funcionalidad).
 */
public class FrameworkDetector implements IFrameworkDetector {

    @Override
    public FrameworkType detectFunctionalFramework(ProjectMetadata metadata) {
        File projectDir = new File(metadata.getPath());

        // Paso 1: Detectar framework
        FrameworkType framework = detectFramework(projectDir);

        if (framework == FrameworkType.UNKNOWN) {
            return null;
        }

        // Paso 2: Validar funcionalidad via IFrameworkStrategy
        try {
            IFrameworkStrategy strategy = FrameworkRegistry.getStrategy(framework);
            boolean isFunctional = strategy.isFunctional(projectDir, metadata.getName());

            if (!isFunctional) {
                return null;
            }
        } catch (IllegalStateException e) {
            return null;
        }

        System.out.println("   [Detector] " + metadata.getName() + " — " + framework.getDisplayName() + " ✅");
        return framework;
    }

    /**
     * Detecta el framework de un proyecto analizando build files y código fuente.
     * Lógica original de FrameworkDetector.detectFramework() sin cambios funcionales.
     */
    public static FrameworkType detectFramework(File projectDir) {
        FrameworkType byDependencies = detectByBuildFile(projectDir);
        if (byDependencies != FrameworkType.UNKNOWN) {
            return byDependencies;
        }
        return detectBySourceCode(projectDir);
    }

    private static FrameworkType detectByBuildFile(File projectDir) {
        File pomXml = new File(projectDir, "pom.xml");
        File buildGradle = new File(projectDir, "build.gradle");
        File buildGradleKts = new File(projectDir, "build.gradle.kts");

        if (pomXml.exists()) {
            try {
                String content = Files.readString(pomXml.toPath());

                if (content.contains("spring-boot-starter") ||
                        content.contains("org.springframework.boot")) {
                    return FrameworkType.SPRING_BOOT;
                }

                if (content.contains("io.quarkus") ||
                        content.contains("quarkus-universe-bom") ||
                        content.contains("quarkus-maven-plugin")) {
                    return FrameworkType.JAKARTA_EE;
                }

                if (content.contains("jakarta.jakartaee-api") ||
                        content.contains("jakarta.platform") ||
                        content.contains("jakarta.ws.rs") ||
                        content.contains("javax.ws.rs") ||
                        content.contains("javaee-api") ||
                        content.contains("javaee-web-api") ||
                        content.contains("microprofile")) {
                    return FrameworkType.JAKARTA_EE;
                }
            } catch (IOException e) {
                // Ignorar
            }
        }

        if (buildGradle.exists() || buildGradleKts.exists()) {
            try {
                File gradle = buildGradle.exists() ? buildGradle : buildGradleKts;
                String content = Files.readString(gradle.toPath());

                if (content.contains("spring-boot") ||
                        content.contains("org.springframework.boot")) {
                    return FrameworkType.SPRING_BOOT;
                }

                if (content.contains("io.quarkus") ||
                        content.contains("quarkusPlatformVersion") ||
                        content.contains("quarkusPlatformGroupId")) {
                    return FrameworkType.JAKARTA_EE;
                }

                if (content.contains("jakarta.jakartaee") ||
                        content.contains("jakarta.ws.rs") ||
                        content.contains("javax.ws.rs") ||
                        content.contains("javaee-api") ||
                        content.contains("microprofile")) {
                    return FrameworkType.JAKARTA_EE;
                }
            } catch (IOException e) {
                // Ignorar
            }
        }

        return FrameworkType.UNKNOWN;
    }

    private static FrameworkType detectBySourceCode(File projectDir) {
        File srcDir = new File(projectDir, "src/main/java");
        if (!srcDir.exists()) {
            return FrameworkType.UNKNOWN;
        }

        try (Stream<Path> walk = Files.walk(srcDir.toPath())) {
            List<Path> javaFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .limit(50)
                    .collect(Collectors.toList());

            int springCount = 0;
            int jakartaCount = 0;
            int quarkusCount = 0;

            for (Path javaFile : javaFiles) {
                try {
                    String content = Files.readString(javaFile);

                    if (content.contains("import org.springframework")) springCount++;
                    if (content.contains("import io.quarkus") ||
                            content.contains("import org.eclipse.microprofile")) quarkusCount++;
                    if (content.contains("import jakarta.ws.rs") ||
                            content.contains("import jakarta.ejb") ||
                            content.contains("import jakarta.enterprise") ||
                            content.contains("import javax.ws.rs") ||
                            content.contains("import javax.ejb") ||
                            content.contains("import javax.enterprise")) jakartaCount++;
                } catch (IOException e) {
                    // Ignorar
                }
            }

            if (quarkusCount > 0) return FrameworkType.JAKARTA_EE;
            if (springCount > jakartaCount && springCount > 0) return FrameworkType.SPRING_BOOT;
            else if (jakartaCount > springCount && jakartaCount > 0) return FrameworkType.JAKARTA_EE;
        } catch (IOException e) {
            // Ignorar
        }

        return FrameworkType.UNKNOWN;
    }
}

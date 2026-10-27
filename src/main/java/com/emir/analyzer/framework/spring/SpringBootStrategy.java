package com.emir.analyzer.framework.spring;

import com.emir.analyzer.config.ConfiguracionFronteras;
import com.emir.analyzer.config.FrameworkConfig;
import com.emir.analyzer.framework.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Estrategia completa para proyectos Spring Boot.
 *
 * VERSIÓN 2.0: Ahora lee configuración desde fronteras.json.
 */
public class SpringBootStrategy implements IFrameworkStrategy {

    private final SpringBootAdapter adapter;
    private final ConfigurableAnnotationMapper annotationMapper;
    private final SpringComponentScanner componentScanner;
    private final FrameworkConfig config;

    /**
     * Constructor que recibe la configuración completa.
     *
     * @param fronteras Configuración cargada desde fronteras.json
     */
    public SpringBootStrategy(ConfiguracionFronteras fronteras) {
        this.config = fronteras.getFrameworkConfig("spring_boot");

        if (this.config == null) {
            throw new IllegalStateException(
                    "No se encontró configuración de 'spring_boot' en fronteras.json"
            );
        }

        this.annotationMapper = new ConfigurableAnnotationMapper(config.getAnnotations());
        this.componentScanner = new SpringComponentScanner();
        this.adapter = new SpringBootAdapter(annotationMapper, componentScanner);
    }

    @Override
    public FrameworkType getFrameworkType() {
        return FrameworkType.SPRING_BOOT;
    }

    @Override
    public String getDisplayName() {
        return "Spring Boot";
    }

    @Override
    public boolean isFunctional(File projectPath, String projectName) {
        File srcDir = new File(projectPath, "src/main/java");

        if (!srcDir.exists()) {
            return false;
        }

        String lowerName = projectName.toLowerCase();

        // Filtro temprano por nombre (Spring Cloud infrastructure)
        if (lowerName.endsWith("-eureka-server") ||
                lowerName.endsWith("-discovery-server") ||
                lowerName.endsWith("-config-server") ||
                lowerName.endsWith("-gateway") ||
                lowerName.endsWith("-admin-server")) {
            return false;
        }

        try {
            boolean hasRestEndpoints = false;
            boolean isInfrastructure = false;
            boolean hasBusinessLogic = false;

            List<Path> javaFiles = Files.walk(srcDir.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path javaFile : javaFiles) {
                String content = new String(Files.readAllBytes(javaFile));

                // Detectar endpoints REST de Spring Boot
                if (!hasRestEndpoints) {
                    if (content.contains("@RestController") ||
                            content.contains("@GetMapping") ||
                            content.contains("@PostMapping") ||
                            content.contains("@PutMapping") ||
                            content.contains("@DeleteMapping") ||
                            content.contains("@PatchMapping") ||
                            content.contains("@RequestMapping")) {
                        hasRestEndpoints = true;
                    }
                }

                // Detectar infraestructura de Spring Cloud (desde config)
                if (!isInfrastructure) {
                    for (String annotation : getInfrastructureAnnotations()) {
                        if (content.contains(annotation)) {
                            isInfrastructure = true;
                            break;
                        }
                    }
                }

                // Detectar lógica de negocio de Spring Boot
                if (!hasBusinessLogic) {
                    if (content.contains("@Service") ||
                            content.contains("@Repository") ||
                            content.contains("@Entity") ||
                            content.contains("JpaRepository") ||
                            content.contains("@Transactional")) {
                        hasBusinessLogic = true;
                    }
                }
            }

            // Decisión final
            if (!hasRestEndpoints) {
                return false;
            }

            if (isInfrastructure) {
                return false;
            }

            if (lowerName.contains("client") && !hasBusinessLogic) {
                return false;
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isInfrastructure(Path buildFilePath, File srcDir) {
        try {
            String content = Files.readString(buildFilePath);

            // Verificar dependencias de infraestructura (desde config)
            for (String dep : getInfrastructureDependencies()) {
                if (content.contains(dep)) {
                    // Si tiene dependencia de gateway pero también tiene controllers, NO es infra pura
                    if (dep.contains("gateway")) {
                        try {
                            return !hasRestControllers(srcDir);
                        } catch (IOException e) {
                            return true;
                        }
                    }
                    return true;
                }
            }

            // Verificar anotaciones de infraestructura en código fuente
            return hasInfrastructureAnnotations(srcDir);

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String[] getInfrastructureDependencies() {
        return config.getInfrastructure().getDependencies().toArray(new String[0]);
    }

    @Override
    public String[] getInfrastructureAnnotations() {
        return config.getInfrastructure().getAnnotations().toArray(new String[0]);
    }

    @Override
    public IFrameworkAdapter getAdapter() {
        return adapter;
    }

    @Override
    public IAnnotationMapper getAnnotationMapper() {
        return annotationMapper;
    }

    @Override
    public IComponentScanner getComponentScanner() {
        return componentScanner;
    }

    // ========== MÉTODOS AUXILIARES ==========

    private boolean hasRestControllers(File srcDir) throws IOException {
        return Files.walk(srcDir.toPath())
                .filter(p -> p.toString().endsWith(".java"))
                .anyMatch(p -> {
                    try {
                        String content = Files.readString(p);
                        return content.contains("@RestController") ||
                                content.contains("@Controller");
                    } catch (IOException e) {
                        return false;
                    }
                });
    }

    private boolean hasInfrastructureAnnotations(File srcDir) {
        try {
            return Files.walk(srcDir.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .anyMatch(p -> {
                        try {
                            String content = Files.readString(p);
                            for (String annotation : getInfrastructureAnnotations()) {
                                if (content.contains(annotation)) {
                                    return true;
                                }
                            }
                        } catch (IOException e) {
                            // Ignorar
                        }
                        return false;
                    });
        } catch (IOException e) {
            return false;
        }
    }
}
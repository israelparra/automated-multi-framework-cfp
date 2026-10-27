package com.emir.analyzer.framework.jakarta;

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
 * Estrategia completa para proyectos Jakarta EE (y legacy Java EE).
 *
 * VERSIÓN 2.0: Ahora lee configuración desde fronteras.json.
 */
public class JakartaStrategy implements IFrameworkStrategy {

    private final JakartaAdapter adapter;
    private final ConfigurableAnnotationMapper annotationMapper;
    private final JakartaComponentScanner componentScanner;
    private final FrameworkConfig config;

    /**
     * Constructor que recibe la configuración completa.
     *
     * @param fronteras Configuración cargada desde fronteras.json
     */
    public JakartaStrategy(ConfiguracionFronteras fronteras) {
        this.config = fronteras.getFrameworkConfig("jakarta_ee");

        if (this.config == null) {
            throw new IllegalStateException(
                    "No se encontró configuración de 'jakarta_ee' en fronteras.json"
            );
        }

        this.annotationMapper = new ConfigurableAnnotationMapper(config.getAnnotations());
        this.componentScanner = new JakartaComponentScanner();
        this.adapter = new JakartaAdapter(annotationMapper, componentScanner);
    }

    @Override
    public FrameworkType getFrameworkType() {
        return FrameworkType.JAKARTA_EE;
    }

    @Override
    public String getDisplayName() {
        return "Jakarta EE";
    }

    @Override
    public boolean isFunctional(File projectPath, String projectName) {
        File srcDir = new File(projectPath, "src/main/java");

        if (!srcDir.exists()) {
            System.out.println("   [DEBUG] [JAKARTA] " + projectName + " rechazado: NO tiene src/main/java");
            return false;
        }

        String lowerName = projectName.toLowerCase();

        // Filtro temprano por nombre exacto (Jakarta EE)
        if (lowerName.equals("tools.descartes.teastore.registryclient") ||
                lowerName.equals("registryclient") ||
                lowerName.endsWith("registryclient") ||
                lowerName.endsWith("-client")) {
            System.out.println("   [DEBUG] [JAKARTA] " + projectName + " rechazado: Cliente de registro (infraestructura)");
            return false;
        }

        try {
            boolean hasRestEndpoints = false;
            boolean isServiceRegistry = false;
            boolean hasBusinessLogic = false;

            List<Path> javaFiles = Files.walk(srcDir.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path javaFile : javaFiles) {
                String content = new String(Files.readAllBytes(javaFile));

                // Detectar endpoints REST de Jakarta EE (moderno y legacy)
                if (!hasRestEndpoints) {
                    if ((content.contains("@Path") &&
                            (content.contains("import jakarta.ws.rs") ||
                                    content.contains("import javax.ws.rs")))) {
                        hasRestEndpoints = true;
                    }
                }

                // Detectar Service Registry (infraestructura Jakarta)
                if (!isServiceRegistry) {
                    if (content.contains("ServiceRegistry") &&
                            content.contains("register") &&
                            !content.contains("@Path")) {
                        isServiceRegistry = true;
                    }
                }

                // Detectar lógica de negocio de Jakarta EE
                if (!hasBusinessLogic) {
                    if (content.contains("EntityManager") ||
                            content.contains("@Stateless") ||
                            content.contains("@Stateful") ||
                            content.contains("@Entity") ||
                            content.contains("DataSource")) {
                        hasBusinessLogic = true;
                    }

                    if (content.contains("Files.readAllBytes") ||
                            content.contains("Files.write")) {
                        hasBusinessLogic = true;
                    }
                }
            }

            // Decisión final
            if (!hasRestEndpoints) {
                System.out.println("   [DEBUG] [JAKARTA] " + projectName + " rechazado: NO tiene endpoints REST");
                return false;
            }

            if (isServiceRegistry) {
                System.out.println("   [DEBUG] [JAKARTA] " + projectName + " rechazado: Es Service Registry");
                return false;
            }

            if ((lowerName.contains("client") ||
                    lowerName.contains("registry")) &&
                    !hasBusinessLogic) {
                System.out.println("   [DEBUG] [JAKARTA] " + projectName + " rechazado: Infraestructura sin lógica de negocio");
                return false;
            }

            return true;

        } catch (IOException e) {
            System.out.println("   [DEBUG] [JAKARTA] " + projectName + " rechazado: Error leyendo archivos");
            return false;
        }
    }

    @Override
    public boolean isInfrastructure(Path buildFilePath, File srcDir) {
        // Jakarta generalmente no tiene componentes de infraestructura específicos
        try {
            return Files.walk(srcDir.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .anyMatch(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("ServiceRegistry") &&
                                    content.contains("register") &&
                                    !content.contains("@Path");
                        } catch (IOException e) {
                            return false;
                        }
                    });
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
}
package com.emir.analyzer.framework;

import com.emir.analyzer.config.ConfiguracionFronteras;
import com.emir.analyzer.config.ConfiguracionFronterasLoader;
import com.emir.analyzer.heuristics.HeuristicConfig;
import com.emir.analyzer.framework.jakarta.JakartaStrategy;
import com.emir.analyzer.framework.spring.SpringBootStrategy;

import java.io.File;
import java.util.*;

/**
 * Registry de frameworks con resolveFrameworkContext().
 * Subsistema: Framework Abstraction Layer.
 *
 * Corresponde a los mensajes 5-8 del diagrama de secuencia:
 * resolveFrameworkContext(frameworkType) → FrameworkContext
 *   internamente: loadForFramework(frameworkName) → ConfiguracionFronteras (msgs 6-7)
 *
 * Lógica de inicialización IDÉNTICA al original.
 */
public class FrameworkRegistry {

    private static final Map<FrameworkType, IFrameworkStrategy> strategies = new HashMap<>();
    private static final Map<FrameworkType, ConfiguracionFronteras> frameworkConfigs = new HashMap<>();
    private static boolean initialized = false;

    private static synchronized void ensureInitialized() {
        if (initialized) return;

        try {
            validateConfigurationFiles();

            // Msg 6-7: loadForFramework (delegado a Configuration subsystem)
            ConfiguracionFronteras springConfig = ConfiguracionFronterasLoader.loadForFramework("spring_boot");
            frameworkConfigs.put(FrameworkType.SPRING_BOOT, springConfig);

            ConfiguracionFronteras jakartaConfig = ConfiguracionFronterasLoader.loadForFramework("jakarta_ee");
            frameworkConfigs.put(FrameworkType.JAKARTA_EE, jakartaConfig);

            HeuristicConfig.initialize(springConfig.getHeuristicEngine());

            register(new SpringBootStrategy(springConfig));
            register(new JakartaStrategy(jakartaConfig));

            initialized = true;

        } catch (Exception e) {
            System.err.println("❌ Error al inicializar frameworks: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("No se pudieron registrar los frameworks", e);
        }
    }

    /**
     * Resuelve el contexto completo de un framework.
     * Corresponde al mensaje 5 del diagrama de secuencia.
     *
     * Internamente invoca loadForFramework() (msgs 6-7) para obtener ConfiguracionFronteras,
     * luego construye el adapter apropiado y retorna FrameworkContext.
     *
     * @param frameworkType Tipo de framework detectado
     * @return FrameworkContext con adapter + config empaquetados
     */
    public static FrameworkContext resolveFrameworkContext(FrameworkType frameworkType) {
        ensureInitialized();

        IFrameworkStrategy strategy = strategies.get(frameworkType);
        if (strategy == null) {
            throw new IllegalStateException("No hay estrategia registrada para: " + frameworkType);
        }

        IFrameworkAdapter adapter = strategy.getAdapter();
        ConfiguracionFronteras config = frameworkConfigs.get(frameworkType);

        return new FrameworkContext(adapter, config);
    }

    private static void validateConfigurationFiles() {
        List<String> missingFiles = new ArrayList<>();

        if (!fileExists("config/common/common-configuration.json") &&
                !fileExists("config/common/common-fronteras.json")) {
            missingFiles.add("config/common/common-configuration.json (o common-fronteras.json)");
        }
        if (!fileExists("config/frameworks/springboot-configuration.json") &&
                !fileExists("config/frameworks/spring_boot-configuration.json") &&
                !fileExists("config/frameworks/spring_boot-fronteras.json")) {
            missingFiles.add("config/frameworks/springboot-configuration.json (o variantes)");
        }
        if (!fileExists("config/frameworks/jakartaee-configuration.json") &&
                !fileExists("config/frameworks/jakarta_ee-configuration.json") &&
                !fileExists("config/frameworks/jakarta_ee-fronteras.json")) {
            missingFiles.add("config/frameworks/jakartaee-configuration.json (o variantes)");
        }

        if (!missingFiles.isEmpty()) {
            throw new RuntimeException(
                    "\n❌ ARCHIVOS DE CONFIGURACIÓN FALTANTES:\n\n" +
                            String.join("\n", missingFiles.stream().map(f -> "   ✗ " + f).toArray(String[]::new)) +
                            "\n\n📝 Solución: Crear los archivos en config/common/ y config/frameworks/\n");
        }
    }

    private static boolean fileExists(String path) { return new File(path).exists(); }

    public static void register(IFrameworkStrategy strategy) {
        if (strategy == null) throw new IllegalArgumentException("Strategy no puede ser null");
        FrameworkType type = strategy.getFrameworkType();
        strategies.put(type, strategy);
    }

    public static IFrameworkStrategy getStrategy(FrameworkType frameworkType) {
        ensureInitialized();
        IFrameworkStrategy strategy = strategies.get(frameworkType);
        if (strategy == null) {
            throw new IllegalStateException("No hay estrategia registrada para: " + frameworkType +
                    ". Disponibles: " + strategies.keySet());
        }
        return strategy;
    }

    public static ConfiguracionFronteras getConfig(FrameworkType frameworkType) {
        ensureInitialized();
        return frameworkConfigs.get(frameworkType);
    }

    public static boolean hasStrategy(FrameworkType frameworkType) { ensureInitialized(); return strategies.containsKey(frameworkType); }
    public static Set<FrameworkType> getSupportedFrameworks() { ensureInitialized(); return strategies.keySet(); }
    public static void clear() { strategies.clear(); frameworkConfigs.clear(); initialized = false; }
    public static void reinitialize() { clear(); ensureInitialized(); }
}

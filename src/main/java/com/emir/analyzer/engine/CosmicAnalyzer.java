package com.emir.analyzer.engine;

import com.emir.analyzer.annotations.LambdaAnnotationCache;
import com.emir.analyzer.artifact.PreparedArtifact;
import com.emir.analyzer.config.ConfiguracionFronteras;
import com.emir.analyzer.framework.IFrameworkAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Implementación de ICosmicAnalyzer.
 * Subsistema: COSMIC Core Engine.
 *
 * Extraída de SourceCodeAnalyzer.analyzeJar().
 * Recibe los 3 insumos ya resueltos y ejecuta el análisis COSMIC puro.
 *
 * Corresponde a los mensajes 13-19 del diagrama de secuencia.
 * Internamente invoca:
 *   - IPersistenceHeuristicEngine (msgs 14-15) via RecursiveCallCounter
 *   - Lambda Annotation Module (msgs 16-17) via LambdaAnnotationCache
 *   - exportReport() (msg 18) como self-message
 */
public class CosmicAnalyzer implements ICosmicAnalyzer {

    private String outputDirectory = "cosmic-reports";

    public CosmicAnalyzer() {}

    public CosmicAnalyzer(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public CosmicFunctionalModel analyze(PreparedArtifact artifact, IFrameworkAdapter adapter,
                                         ConfiguracionFronteras config) {

        URLClassLoader classLoader = artifact.getClassLoader();
        Set<String> allClassNames = artifact.getAllClassNames();
        List<Class<?>> loadedClasses = artifact.getLoadedClasses();

        // Fase 1: Auto-descubrir paquetes
        Class<?> mainAppClass = null;
        for (Class<?> clazz : loadedClasses) {
            if (adapter.isMainApplicationClass(clazz)) {
                mainAppClass = clazz;
                break;
            }
        }

        Set<String> finalScanPackages = adapter.getComponentScanner().autoDiscoverPackages(mainAppClass);
        if (finalScanPackages.isEmpty()) {
            System.out.println("⚠️ No se detectó clase principal. Activando modo FALLBACK...");
            finalScanPackages = fallbackPackageDiscovery(loadedClasses);
        }

        if (finalScanPackages.isEmpty()) {
            System.err.println("❌ No se pudieron determinar paquetes para escanear.");
            return null;
        }

        // Fase 1.5: Encontrar repositorios
        Set<String> repositoryInterfaceNames = new HashSet<>();
        for (Class<?> clazz : loadedClasses) {
            String internalName = clazz.getName().replace('.', '/');
            boolean shouldScan = finalScanPackages.stream().anyMatch(internalName::startsWith);
            if (shouldScan && adapter.isDataRepository(clazz)) {
                repositoryInterfaceNames.add(internalName);
            }
        }

        // Iniciar analizador de bytecode
        BytecodeAnalyzer bytecodeAnalyzer = new BytecodeAnalyzer(
                classLoader, finalScanPackages, repositoryInterfaceNames, config, allClassNames);

        // Fase 1.7: Inicializar caché @CosmicLambda (msg 16-17)
        LambdaAnnotationCache.getInstance().initialize(classLoader, allClassNames, finalScanPackages);

        // Crear detector universal
        UniversalComponentDetector universalComponentDetector = new UniversalComponentDetector(bytecodeAnalyzer, adapter);

        // Fase 2: Analizar Controladores REST y Entidades
        System.out.println("\n\uD83D\uDD0D Analizando Endpoints y Entidades...");
        Map<String, ClassMetaData> classMetaDataMap = new HashMap<>();

        for (Class<?> clazz : loadedClasses) {
            String internalName = clazz.getName().replace('.', '/');
            boolean shouldScan = finalScanPackages.stream().anyMatch(internalName::startsWith);
            if (!shouldScan) continue;

            if (adapter.isRestController(clazz)) {
                try {
                    ClassMetaData metaData = createMetaClass(clazz, universalComponentDetector, adapter);
                    int endpointCount = metaData.getMethods().size();
                    System.out.println("\n   [RestController] " + clazz.getSimpleName() +
                            " \u2014 " + endpointCount + " endpoints detectados");
                    classMetaDataMap.put(clazz.getName(), metaData);
                    System.out.println("   \u2705 Analizado correctamente: " + clazz.getSimpleName());
                } catch (Exception e) {
                    System.out.println("   \u274C Error al crear metadatos para: " + clazz.getSimpleName());
                    e.printStackTrace();
                }
            }
        }

        // Fase 3: Generar modelo COSMIC
        CosmicFunctionalModel cosmicModel = new CosmicFunctionalModel(universalComponentDetector);
        cosmicModel.identifyMovements();
        cosmicModel.generateReport();

        System.out.println("\n✨ ANÁLISIS COMPLETADO");
        System.out.println("📊 Framework: " + adapter.getFrameworkType().getDisplayName());
        System.out.println("📊 Total de clases analizadas: " + classMetaDataMap.size());
        System.out.println("📊 Total CFP calculados: " + cosmicModel.calculateTotalCFP());

        return cosmicModel;
    }

    private Set<String> fallbackPackageDiscovery(List<Class<?>> loadedClasses) {
        Set<String> allCandidatePackages = new HashSet<>();
        for (Class<?> clazz : loadedClasses) {
            if (clazz.getPackage() != null) {
                allCandidatePackages.add(clazz.getPackage().getName().replace('.', '/'));
            }
        }

        Set<String> projectPackages = new HashSet<>();
        for (String pkg : allCandidatePackages) {
            if (!isLibraryPackage(pkg)) projectPackages.add(pkg);
        }

        Set<String> optimizedPackages = new HashSet<>();
        List<String> sorted = new ArrayList<>(projectPackages);
        Collections.sort(sorted);
        for (String pkg : sorted) {
            boolean isSubPackage = false;
            for (String existing : optimizedPackages) {
                if (pkg.startsWith(existing + "/")) { isSubPackage = true; break; }
            }
            if (!isSubPackage) optimizedPackages.add(pkg);
        }

        return optimizedPackages;
    }

    private boolean isLibraryPackage(String pkg) {
        if (pkg.startsWith("java/") || pkg.startsWith("javax/") || pkg.startsWith("jdk/") ||
                pkg.startsWith("sun/") || pkg.startsWith("com/sun/")) return true;
        String[] knownFrameworks = {
                "org/springframework/", "org/apache/", "org/eclipse/microprofile/",
                "org/jboss/", "org/hibernate/", "org/slf4j/", "org/junit/",
                "io/quarkus/", "io/smallrye/", "io/vertx/", "jakarta/",
                "com/fasterxml/jackson/", "com/google/", "org/objectweb/asm/"
        };
        for (String framework : knownFrameworks) { if (pkg.startsWith(framework)) return true; }
        return false;
    }

    private ClassMetaData createMetaClass(Class<?> clazz, UniversalComponentDetector detector,
                                          IFrameworkAdapter adapter) {
        ClassMetaData meta = new ClassMetaData();
        meta.setClassName(clazz.getName());
        meta.setAnnotations(new ArrayList<>());
        meta.setMethods(new ArrayList<>());
        meta.setFields(new ArrayList<>());
        meta.setDependencies(new ArrayList<>());

        try { meta.setAnnotations(detector.getClassAnnotations(clazz)); }
        catch (Throwable t) { System.err.println("   ⚠️ Error al obtener anotaciones: " + t.getMessage()); }

        try {
            List<Method> methods = detector.getClassMethodsWithAPIAnnotation(clazz);
            meta.setMethods(methods);
        } catch (Throwable t) { System.err.println("   ⚠️ Error al obtener métodos: " + t.getMessage()); }

        try { detector.classifyCosmicMovements(meta); }
        catch (Throwable t) {
            System.err.println("   ❌ Error en análisis COSMIC: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            Field[] declaredFields = clazz.getDeclaredFields();
            meta.setFields(Arrays.asList(declaredFields));
        } catch (Throwable t) { /* Silencioso */ }

        return meta;
    }
}

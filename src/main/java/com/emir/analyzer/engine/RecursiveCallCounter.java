package com.emir.analyzer.engine;

import com.emir.analyzer.annotations.CosmicLambdaDetector;
import com.emir.analyzer.annotations.LambdaAnnotationCache;
import com.emir.analyzer.config.ConfiguracionFronteras;
import com.emir.analyzer.heuristics.PersistenceHeuristicEngine;
import org.objectweb.asm.*;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Visitor ASM que cuenta llamadas recursivas y clasifica movimientos COSMIC.
 * Subsistema: COSMIC Core Engine.
 * Movido de com.emir.analyzer. Lógica IDÉNTICA.
 *
 * Dependencias permitidas:
 * - IPersistenceHeuristicEngine (msgs 14-15 del diagrama)
 * - Lambda Annotation Module (msgs 16-17 del diagrama)
 * - Configuration (vía parámetro config)
 */
public class RecursiveCallCounter extends ClassVisitor {

    private final BytecodeAnalyzer analyzer;
    private final AnalysisResult result;
    private final String targetMethodName;
    private final String targetMethodDescriptor;
    private final Set<String> visitedSignatures;
    private final Set<String> repositoryInterfaceNames;
    private final Set<String> scanPackages;
    private final ConfiguracionFronteras config;
    private final PersistenceHeuristicEngine persistenceHeuristic;
    private String currentClassName;
    // COSMIC dedup: same file boundary fired more than once in the same endpoint => 1 movement (same data group).
    // Known limitation: cannot distinguish two physical files written via the same FileWriter signature.
    private final Set<String> firedFileWriteBoundaries = new HashSet<>();
    private final Set<String> firedFileReadBoundaries = new HashSet<>();

    public RecursiveCallCounter(int api, BytecodeAnalyzer analyzer, AnalysisResult result,
                                String targetMethodName, String targetMethodDescriptor,
                                Set<String> visitedSignatures, Set<String> repositoryInterfaceNames,
                                Set<String> scanPackages, ConfiguracionFronteras config) {
        super(api);
        this.analyzer = analyzer;
        this.result = result;
        this.targetMethodName = targetMethodName;
        this.targetMethodDescriptor = targetMethodDescriptor;
        this.visitedSignatures = visitedSignatures;
        this.repositoryInterfaceNames = repositoryInterfaceNames;
        this.scanPackages = scanPackages;
        this.config = config;
        this.persistenceHeuristic = new PersistenceHeuristicEngine(scanPackages);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.currentClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals(targetMethodName) && descriptor.equals(targetMethodDescriptor)) {
            return new MethodVisitor(api) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    handleMethodCall(opcode, owner, name, descriptor, isInterface);
                }
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) { }
                @Override
                public void visitEnd() {
                    applyLambdaAnnotations();
                    super.visitEnd();
                }
            };
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    /**
     * Aplica anotaciones @CosmicLambda.
     * Corresponde a msgs 16-17 del diagrama de secuencia:
     * getLambdaMovements(className, methodName) → List<CosmicLambdaInfo>
     */
    private void applyLambdaAnnotations() {
        CosmicLambdaDetector.LambdaAnnotationInfo annotation =
                LambdaAnnotationCache.getInstance().getAnnotation(
                        currentClassName, targetMethodName, targetMethodDescriptor);
        if (annotation == null) return;

        int readsBefore = result.getReadCount();
        int writesBefore = result.getWriteCount();

        for (String movementType : annotation.movementTypes) {
            for (int i = 0; i < annotation.count; i++) {
                switch (movementType) {
                    case "READ": result.addRead(); break;
                    case "WRITE": result.addWrite(); break;
                }
            }
        }

        System.out.println("   [@CosmicLambda] " + currentClassName.replace('/', '.') + "." +
                targetMethodName + "(): " + annotation.count + " movimiento(s) " +
                annotation.movementTypes + " aplicados" +
                ((readsBefore > 0 || writesBefore > 0) ? " ⚠️ detección mixta" : ""));
    }

    /**
     * Maneja cada llamada a método encontrada en el bytecode.
     * Lógica IDÉNTICA al original (~250 líneas).
     * Internamente invoca persistenceHeuristic.analyze() (msgs 14-15).
     */
    private void handleMethodCall(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        String fullMethodSignature = owner + "." + name;

        // File write boundaries (dedup per endpoint: same boundary => same data group => 1 W)
        if (config.getArchivosWriteFronteras() != null) {
            for (String frontera : config.getArchivosWriteFronteras()) {
                if (fullMethodSignature.startsWith(frontera)) {
                    if (firedFileWriteBoundaries.add(frontera)) {
                        result.addWrite();
                    }
                    return;
                }
            }
        }

        // File read boundaries (dedup per endpoint: same boundary => same data group => 1 R)
        if (config.getArchivosReadFronteras() != null) {
            for (String frontera : config.getArchivosReadFronteras()) {
                if (fullMethodSignature.startsWith(frontera)) {
                    if (firedFileReadBoundaries.add(frontera)) {
                        result.addRead();
                    }
                    return;
                }
            }
        }

        // Thread.start()
        if (owner.equals("java/lang/Thread") && name.equals("start") && descriptor.equals("()V")) {
            analyzeThreadRunMethods(); return;
        }

        // EntityManager
        if (owner.equals("jakarta/persistence/EntityManager") || owner.equals("javax/persistence/EntityManager")) {
            if (name.equals("persist") || name.equals("merge") || name.equals("remove")) {
                System.out.println("   [BYTECODE] WRITE Base Case: EntityManager." + name); result.addWrite(); return;
            }
            if (name.equals("find") || name.equals("getReference") || name.equals("createQuery") ||
                    name.equals("createNamedQuery") || name.equals("createNativeQuery")) {
                System.out.println("   [BYTECODE] READ Base Case: EntityManager." + name); result.addRead(); return;
            }
        }

        // JPA Query
        if (owner.equals("jakarta/persistence/Query") || owner.equals("javax/persistence/Query") ||
                owner.equals("jakarta/persistence/TypedQuery") || owner.equals("javax/persistence/TypedQuery")) {
            if (name.equals("getResultList") || name.equals("getSingleResult") ||
                    name.equals("getFirstResult") || name.equals("stream")) {
                System.out.println("   [BYTECODE] READ Base Case: Query." + name); result.addRead(); return;
            }
            if (name.equals("executeUpdate")) {
                System.out.println("   [BYTECODE] WRITE Base Case: Query.executeUpdate"); result.addWrite(); return;
            }
        }

        // Repository methods
        if (isRepositoryMethod(owner, name)) {
            if (isReadRepositoryMethod(name)) {
                System.out.println("   [BYTECODE] READ Base Case: Repository." + name); result.addRead(); return;
            }
            if (isWriteRepositoryMethod(name)) {
                System.out.println("   [BYTECODE] WRITE Base Case: Repository." + name); result.addWrite(); return;
            }
        }

        // Custom Repository
        if (owner.contains("Repository") || owner.endsWith("Repository")) {
            String methodNameLower = name.toLowerCase();
            if (methodNameLower.equals("add") || methodNameLower.equals("save") || methodNameLower.equals("create") ||
                    methodNameLower.equals("insert") || methodNameLower.equals("update") ||
                    methodNameLower.equals("delete") || methodNameLower.equals("remove")) {
                System.out.println("   [BYTECODE] WRITE Base Case: Custom Repository." + name); result.addWrite(); return;
            }
            if (methodNameLower.startsWith("find") || methodNameLower.startsWith("get") ||
                    methodNameLower.equals("findall") || methodNameLower.startsWith("findby")) {
                System.out.println("   [BYTECODE] READ Base Case: Custom Repository." + name); result.addRead(); return;
            }
        }

        // HTTP Client boundaries
        if (config.getApiFronteras() != null && config.getApiFronteras().contains(owner)) {
            if (name.equals("execute") || name.equals("exchange") || name.equals("getForEntity") ||
                    name.equals("getForObject") || name.equals("retrieve") || name.equals("get")) {
                System.out.println("   [BYTECODE] READ Base Case (HTTP Client): " + owner + "." + name); result.addRead(); return;
            }
            if (name.equals("postForEntity") || name.equals("postForObject") || name.equals("put") ||
                    name.equals("delete") || name.equals("patch") || name.equals("post")) {
                System.out.println("   [BYTECODE] WRITE Base Case (HTTP Client): " + owner + "." + name); result.addWrite(); return;
            }
        }

        // External API heuristic (msg 14-15: classifyPersistence)
        if (!isInternalClass(owner)) {
            PersistenceHeuristicEngine.PersistenceType type = persistenceHeuristic.analyze(owner, name, descriptor);
            switch (type) {
                case READ:
                    System.out.println("   [BYTECODE] READ Base Case (External API - Heuristic): " + owner.replace('/', '.') + "." + name);
                    result.addRead(); return;
                case WRITE:
                    System.out.println("   [BYTECODE] WRITE Base Case (External API - Heuristic): " + owner.replace('/', '.') + "." + name);
                    result.addWrite(); return;
                case NONE: return;
            }
        }

        // Interface dispatch
        if (isInterface && opcode == Opcodes.INVOKEINTERFACE && isInternalClass(owner)) {
            if (isQuarkusRestClient(owner)) {
                System.out.println("   [QUARKUS REST CLIENT] Detectado: " + owner.replace('/', '.') + "." + name);
                result.addRead();
                System.out.println("   [QUARKUS REST CLIENT] ✅ 1 EXIT detectado"); return;
            }

            if (isFeignClient(owner)) {
                String className = owner.replace('/', '.');
                System.out.println("   [FEIGN] Detectado Feign Client: " + className + "." + name);
                String mn = name.toLowerCase();
                if (mn.startsWith("find") || mn.startsWith("get") || mn.equals("findall") || mn.equals("findbyid")) {
                    result.addRead(); result.addRead();
                    System.out.println("   [FEIGN] ✅ 2 READs detectados"); return;
                }
                if (mn.startsWith("save") || mn.startsWith("create") || mn.startsWith("update") || mn.startsWith("delete")) {
                    result.addWrite(); result.addRead();
                    System.out.println("   [FEIGN] ✅ WRITE + READ detectados"); return;
                }
                result.addRead(); result.addRead();
                System.out.println("   [FEIGN] ✅ 2 READs detectados (método desconocido)"); return;
            }

            Set<String> implementations = analyzer.findImplementations(owner);
            boolean hasRead = false, hasWrite = false;
            for (String implClass : implementations) {
                AnalysisResult implResult = analyzer.analyzeRecursive(implClass, name, descriptor, visitedSignatures);
                if (implResult != null) {
                    if (implResult.getReadCount() > 0) hasRead = true;
                    if (implResult.getWriteCount() > 0) hasWrite = true;
                }
            }
            if (hasRead) result.addRead();
            if (hasWrite) result.addWrite();
            return;
        }

        // Internal entity Active Record
        if (isInternalClass(owner)) {
            PersistenceHeuristicEngine.PersistenceType entityOp = persistenceHeuristic.analyze(owner, name, descriptor);
            if (entityOp != PersistenceHeuristicEngine.PersistenceType.NONE) {
                switch (entityOp) {
                    case READ:
                        System.out.println("   [BYTECODE] READ Base Case (Entity Active Record): " + owner.replace('/', '.') + "." + name);
                        result.addRead(); return;
                    case WRITE:
                        System.out.println("   [BYTECODE] WRITE Base Case (Entity Active Record): " + owner.replace('/', '.') + "." + name);
                        result.addWrite(); return;
                }
            }
        }

        // Recursive internal call
        if (isInternalClass(owner)) {
            AnalysisResult nestedResult = analyzer.analyzeRecursive(owner, name, descriptor, visitedSignatures);
            if (nestedResult != null) {
                result.add(nestedResult);
            }
        }
    }

    private boolean isQuarkusRestClient(String owner) {
        try {
            String className = owner.replace('/', '.');
            Class<?> interfaceClass = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            for (java.lang.annotation.Annotation annotation : interfaceClass.getAnnotations()) {
                if (annotation.annotationType().getName().equals("org.eclipse.microprofile.rest.client.inject.RegisterRestClient")) return true;
            }
        } catch (Throwable t) {
            // fallback to ASM
        }
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(owner + ".class");
            if (is == null) return false;
            ClassReader reader = new ClassReader(is);
            QuarkusRestClientDetector detector = new QuarkusRestClientDetector();
            reader.accept(detector, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();
            if (detector.hasRegisterRestClient()) return true;
        } catch (Exception e) { }
        if (owner.contains("/client/") && owner.endsWith("Client")) {
            if (!owner.contains("/feign/") && !owner.contains("/cloud/")) return true;
        }
        return false;
    }

    private static class QuarkusRestClientDetector extends ClassVisitor {
        private boolean found = false;
        public QuarkusRestClientDetector() { super(Opcodes.ASM9); }
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.contains("microprofile/rest/client") && descriptor.contains("RegisterRestClient")) found = true;
            return null;
        }
        public boolean hasRegisterRestClient() { return found; }
    }

    private boolean isFeignClient(String owner) {
        if (config.getAnotacionesApiFronteras() == null || config.getAnotacionesApiFronteras().isEmpty()) return false;
        try {
            String className = owner.replace('/', '.');
            Class<?> interfaceClass = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            for (java.lang.annotation.Annotation annotation : interfaceClass.getAnnotations()) {
                String annotationName = annotation.annotationType().getName();
                String annotationInternal = annotationName.replace('.', '/');
                for (String feignAnnotation : config.getAnotacionesApiFronteras()) {
                    if (annotationInternal.equals(feignAnnotation) || annotationName.endsWith("FeignClient")) return true;
                }
            }
        } catch (ClassNotFoundException e) {
            String cn = owner.substring(owner.lastIndexOf('/') + 1);
            if (cn.endsWith("ClientService") || cn.endsWith("FeignClient")) {
                if (!owner.contains("/microprofile/") && !owner.contains("/quarkus/")) return true;
            }
        } catch (Throwable t) { }
        return false;
    }

    private void analyzeThreadRunMethods() {
        try {
            for (int i = 1; i <= 10; i++) {
                String innerClassName = currentClassName + "$" + i;
                AnalysisResult threadResult = analyzer.analyzeRecursive(innerClassName, "run", "()V", visitedSignatures);
                if (threadResult != null && (threadResult.getReadCount() > 0 || threadResult.getWriteCount() > 0)) {
                    result.add(threadResult);
                }
            }
        } catch (Exception e) { /* Ignorar errores de análisis de threads */ }
    }

    private boolean isInternalClass(String owner) {
        for (String pkg : scanPackages) { if (owner.startsWith(pkg)) return true; }
        return false;
    }

    private boolean isRepositoryMethod(String owner, String methodName) {
        if (repositoryInterfaceNames.contains(owner)) return true;
        return owner.substring(owner.lastIndexOf('/') + 1).endsWith("Repository");
    }

    private boolean isReadRepositoryMethod(String methodName) {
        String lower = methodName.toLowerCase();
        return config.getNomenclaturaReadPrefijos().stream().anyMatch(lower::startsWith);
    }

    private boolean isWriteRepositoryMethod(String methodName) {
        String lower = methodName.toLowerCase();
        return config.getNomenclaturaWritePrefijos().stream().anyMatch(lower::startsWith);
    }
}

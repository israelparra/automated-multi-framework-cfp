package com.emir.analyzer.engine;

import com.emir.analyzer.framework.IFrameworkAdapter;
import lombok.Getter;
import org.objectweb.asm.*;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Detector universal de componentes COSMIC con soporte multi-framework.
 * Subsistema: COSMIC Core Engine.
 * Movido de com.emir.analyzer. Lógica IDÉNTICA.
 */
@Getter
public class UniversalComponentDetector {

    private Map<CosmicDataMovementType, List<CosmicDataMovement>> rawMovementTypeMap;
    private Set<String> entityAnnotations;
    private Set<String> apiAnnotations;
    private final BytecodeAnalyzer bytecodeAnalyzer;
    private final IFrameworkAdapter frameworkAdapter;

    public UniversalComponentDetector(BytecodeAnalyzer bytecodeAnalyzer, IFrameworkAdapter frameworkAdapter) {
        this.bytecodeAnalyzer = bytecodeAnalyzer;
        this.frameworkAdapter = frameworkAdapter;
        rawMovementTypeMap = new HashMap<>();
        rawMovementTypeMap.put(CosmicDataMovementType.ENTRY, new LinkedList<>());
        rawMovementTypeMap.put(CosmicDataMovementType.EXIT, new LinkedList<>());
        rawMovementTypeMap.put(CosmicDataMovementType.READ, new LinkedList<>());
        rawMovementTypeMap.put(CosmicDataMovementType.WRITE, new LinkedList<>());
        this.apiAnnotations = frameworkAdapter.getAnnotationMapper().getHttpMappingAnnotations();
        this.entityAnnotations = frameworkAdapter.getAnnotationMapper().getEntityAnnotations();
    }

    public void classifyCosmicMovements(ClassMetaData classMetaData) {
        List<Method> methods = classMetaData.getMethods();
        if (!methods.isEmpty() && methods.get(0).getName().equals("dummyMethod")) {
            System.out.println("   \uD83D\uDD0D [Jakarta] Detectados métodos sintéticos, analizando por bytecode...");
            classifyBytecode(classMetaData);
            return;
        }
        methods.forEach(method -> {
            try {
                if (frameworkAdapter.hasRestEndpointAnnotation(method)) {
                    analyzeEndpoint(method, classMetaData.getClassName());
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error al analizar método: " + method.getName());
                e.printStackTrace();
            }
        });
    }

    private void classifyBytecode(ClassMetaData classMetaData) {
        String className = classMetaData.getClassName();
        try {
            String classInternalName = className.replace('.', '/') + ".class";
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream is = loader.getResourceAsStream(classInternalName);
            if (is == null) { System.err.println("      ❌ No se pudo leer bytecode de " + className); return; }
            ClassReader reader = new ClassReader(is);
            JakartaMethodAnalyzer analyzer = new JakartaMethodAnalyzer(className, this);
            reader.accept(analyzer, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();
            System.out.println("      ✅ Análisis por bytecode completado: " + analyzer.getMethodCount() + " métodos procesados");
        } catch (Exception e) {
            System.err.println("      ❌ Error en análisis bytecode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class JakartaMethodAnalyzer extends ClassVisitor {
        private final String className;
        private final UniversalComponentDetector detector;
        private int methodCount = 0;

        public JakartaMethodAnalyzer(String className, UniversalComponentDetector detector) {
            super(Opcodes.ASM9);
            this.className = className;
            this.detector = detector;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                private boolean hasRestAnnotation = false;
                private String httpMethod = "UNKNOWN";
                private String methodName = name;

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (desc.contains("ws/rs/GET")) { hasRestAnnotation = true; httpMethod = "GET"; }
                    else if (desc.contains("ws/rs/POST")) { hasRestAnnotation = true; httpMethod = "POST"; }
                    else if (desc.contains("ws/rs/PUT")) { hasRestAnnotation = true; httpMethod = "PUT"; }
                    else if (desc.contains("ws/rs/DELETE")) { hasRestAnnotation = true; httpMethod = "DELETE"; }
                    else if (desc.contains("ws/rs/Path")) { hasRestAnnotation = true; }
                    return null;
                }

                @Override
                public void visitEnd() {
                    if (hasRestAnnotation) {
                        methodCount++;
                        analyzeEndpointBytecode(methodName, descriptor, className);
                    }
                }
            };
        }

        private void analyzeEndpointBytecode(String methodName, String descriptor, String ownerClass) {
            CosmicDataMovement entry = new CosmicDataMovement(methodName + "_entry", CosmicDataMovementType.ENTRY, ownerClass);
            detector.rawMovementTypeMap.get(CosmicDataMovementType.ENTRY).add(entry);

            org.objectweb.asm.Type returnType = org.objectweb.asm.Type.getReturnType(descriptor);
            if (returnType.getSort() != org.objectweb.asm.Type.VOID) {
                CosmicDataMovement exit = new CosmicDataMovement(methodName + "_exit", CosmicDataMovementType.EXIT, ownerClass);
                detector.rawMovementTypeMap.get(CosmicDataMovementType.EXIT).add(exit);
            }

            String owner = ownerClass.replace('.', '/');
            AnalysisResult counts = detector.bytecodeAnalyzer.analyzeRecursive(owner, methodName, descriptor, new HashSet<>());

            for (int i = 0; i < counts.getReadCount(); i++) {
                detector.rawMovementTypeMap.get(CosmicDataMovementType.READ).add(
                        new CosmicDataMovement(methodName + "_read_" + i, CosmicDataMovementType.READ, ownerClass));
            }
            for (int i = 0; i < counts.getWriteCount(); i++) {
                detector.rawMovementTypeMap.get(CosmicDataMovementType.WRITE).add(
                        new CosmicDataMovement(methodName + "_write_" + i, CosmicDataMovementType.WRITE, ownerClass));
            }
        }

        public int getMethodCount() { return methodCount; }
    }

    private void analyzeEndpoint(Method method, String className) {
        String methodName = method.getName();

        rawMovementTypeMap.get(CosmicDataMovementType.ENTRY).add(
                new CosmicDataMovement(methodName + "_entry", CosmicDataMovementType.ENTRY, method, className));

        if (hasExitMovement(method)) {
            rawMovementTypeMap.get(CosmicDataMovementType.EXIT).add(
                    new CosmicDataMovement(methodName + "_exit", CosmicDataMovementType.EXIT, method, className));
        }

        String owner = method.getDeclaringClass().getName().replace('.', '/');
        String descriptor = org.objectweb.asm.Type.getMethodDescriptor(method);
        AnalysisResult counts = bytecodeAnalyzer.analyzeRecursive(owner, method.getName(), descriptor, new HashSet<>());

        for (int i = 0; i < counts.getReadCount(); i++) {
            rawMovementTypeMap.get(CosmicDataMovementType.READ).add(
                    new CosmicDataMovement(methodName + "_read_" + i, CosmicDataMovementType.READ, method, className));
        }
        for (int i = 0; i < counts.getWriteCount(); i++) {
            rawMovementTypeMap.get(CosmicDataMovementType.WRITE).add(
                    new CosmicDataMovement(methodName + "_write_" + i, CosmicDataMovementType.WRITE, method, className));
        }
    }

    private boolean hasExitMovement(Method method) {
        Class<?> returnType = method.getReturnType();
        // Solo void puro (sin respuesta HTTP) no genera Exit.
        // ResponseEntity<Void> SI genera Exit: el codigo de estado HTTP (200/404/...) es
        // un grupo de datos que cruza la frontera al usuario funcional segun COSMIC v4.0.1, sec 3.2.1.
        if (returnType == void.class) return false;
        return true;
    }

    public List<Method> getClassMethodsWithAPIAnnotation(Class<?> clazz) {
        List<Method> methodList = new LinkedList<>();
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (frameworkAdapter.hasRestEndpointAnnotation(method)) methodList.add(method);
            }
            if (!methodList.isEmpty()) return methodList;
        } catch (Throwable t) {
            System.err.println("   ⚠️ Error al obtener métodos de " + clazz.getName() + ": " + t.getMessage());
        }
        System.out.println("   \uD83D\uDD0D [Jakarta] Analizando métodos por bytecode...");
        return getMethodsFromBytecode(clazz);
    }

    private List<Method> getMethodsFromBytecode(Class<?> clazz) {
        List<Method> methodList = new LinkedList<>();
        try {
            String classInternalName = clazz.getName().replace('.', '/') + ".class";
            InputStream is = clazz.getClassLoader().getResourceAsStream(classInternalName);
            if (is == null) return methodList;
            ClassReader reader = new ClassReader(is);
            RestMethodExtractor extractor = new RestMethodExtractor();
            reader.accept(extractor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();
            int count = extractor.getRestMethodCount();
            System.out.println("      ✅ Encontrados " + count + " métodos REST por bytecode");
            for (int i = 0; i < count; i++) {
                try { methodList.add(UniversalComponentDetector.class.getDeclaredMethod("dummyMethod")); }
                catch (NoSuchMethodException e) { }
            }
        } catch (Exception e) { System.err.println("      ❌ Error leyendo bytecode: " + e.getMessage()); }
        return methodList;
    }

    private static class RestMethodExtractor extends ClassVisitor {
        private int count = 0;
        public RestMethodExtractor() { super(Opcodes.ASM9); }
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (desc.contains("ws/rs/GET") || desc.contains("ws/rs/POST") ||
                            desc.contains("ws/rs/PUT") || desc.contains("ws/rs/DELETE") || desc.contains("ws/rs/Path")) {
                        count++;
                    }
                    return null;
                }
            };
        }
        public int getRestMethodCount() { return count; }
    }

    @SuppressWarnings("unused")
    private static void dummyMethod() { }

    public List<String> getClassAnnotations(Class<?> clazz) {
        List<String> annotationsFound = new LinkedList<>();
        try {
            annotationsFound.addAll(this.apiAnnotations.stream()
                    .filter(a -> Arrays.stream(clazz.getAnnotations())
                            .anyMatch(ca -> ca.annotationType().getName().equalsIgnoreCase(a))).toList());
            annotationsFound.addAll(this.entityAnnotations.stream()
                    .filter(a -> Arrays.stream(clazz.getAnnotations())
                            .anyMatch(ca -> ca.annotationType().getName().equalsIgnoreCase(a))).toList());
        } catch (Throwable t) { }
        return annotationsFound;
    }

    public Map<CosmicDataMovementType, List<CosmicDataMovement>> getMovementTypeMap() {
        boolean shouldDeduplicate = frameworkAdapter.requiresMovementDeduplication();
        if (shouldDeduplicate) {
            return deduplicateAllMovements();
        } else {
            return rawMovementTypeMap;
        }
    }

    private Map<CosmicDataMovementType, List<CosmicDataMovement>> deduplicateAllMovements() {
        Map<String, List<CosmicDataMovement>> movementsByMethod = new HashMap<>();
        for (List<CosmicDataMovement> movements : rawMovementTypeMap.values()) {
            for (CosmicDataMovement movement : movements) {
                String methodName = extractMethodName(movement);
                movementsByMethod.computeIfAbsent(methodName, k -> new ArrayList<>()).add(movement);
            }
        }
        List<CosmicDataMovement> allDeduplicated = new ArrayList<>();
        for (Map.Entry<String, List<CosmicDataMovement>> entry : movementsByMethod.entrySet()) {
            allDeduplicated.addAll(deduplicateByMethod(entry.getValue(), entry.getKey()));
        }
        Map<CosmicDataMovementType, List<CosmicDataMovement>> result = new HashMap<>();
        result.put(CosmicDataMovementType.ENTRY, new LinkedList<>());
        result.put(CosmicDataMovementType.EXIT, new LinkedList<>());
        result.put(CosmicDataMovementType.READ, new LinkedList<>());
        result.put(CosmicDataMovementType.WRITE, new LinkedList<>());
        for (CosmicDataMovement movement : allDeduplicated) { result.get(movement.getType()).add(movement); }
        return result;
    }

    private List<CosmicDataMovement> deduplicateByMethod(List<CosmicDataMovement> movements, String methodName) {
        Map<String, CosmicDataMovement> uniqueMovements = new LinkedHashMap<>();
        for (CosmicDataMovement movement : movements) {
            if (movement.getType() == CosmicDataMovementType.ENTRY || movement.getType() == CosmicDataMovementType.EXIT) {
                uniqueMovements.put(movement.getName(), movement); continue;
            }
            String uniqueKey = methodName + "_" + movement.getType().toString();
            if (!uniqueMovements.containsKey(uniqueKey)) uniqueMovements.put(uniqueKey, movement);
        }
        return new ArrayList<>(uniqueMovements.values());
    }

    private String extractMethodName(CosmicDataMovement movement) {
        String name = movement.getName();
        if (name.contains("_entry")) return name.replace("_entry", "");
        if (name.contains("_exit")) return name.replace("_exit", "");
        if (name.contains("_read_")) return name.substring(0, name.indexOf("_read_"));
        if (name.contains("_write_")) return name.substring(0, name.indexOf("_write_"));
        return name;
    }
}

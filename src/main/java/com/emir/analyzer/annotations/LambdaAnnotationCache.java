package com.emir.analyzer.annotations;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache singleton thread-safe de anotaciones @CosmicLambda.
 * Subsistema: Lambda Annotation Module. Sin cambios.
 */
public class LambdaAnnotationCache {

    private static final LambdaAnnotationCache INSTANCE = new LambdaAnnotationCache();
    private final Map<String, CosmicLambdaDetector.LambdaAnnotationInfo> cache = new ConcurrentHashMap<>();
    private boolean initialized = false;

    private LambdaAnnotationCache() {}

    public static LambdaAnnotationCache getInstance() { return INSTANCE; }

    public void initialize(ClassLoader classLoader, Set<String> allClassNames, Set<String> scanPackages) {
        if (initialized) return;

        int found = 0;

        for (String className : allClassNames) {
            boolean inScanScope = scanPackages.stream().anyMatch(className::startsWith);
            if (!inScanScope) continue;

            try {
                java.io.InputStream is = classLoader.getResourceAsStream(className + ".class");
                if (is == null) continue;

                org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(is);
                MethodScanner scanner = new MethodScanner(classLoader, className);
                reader.accept(scanner, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_FRAMES);
                is.close();

                found += scanner.getFoundCount();
            } catch (Exception e) { /* Ignorar */ }
        }

        initialized = true;
    }

    public CosmicLambdaDetector.LambdaAnnotationInfo getAnnotation(String className, String methodName, String descriptor) {
        String key = className + "." + methodName + descriptor;
        return cache.get(key);
    }

    private class MethodScanner extends org.objectweb.asm.ClassVisitor {
        private final ClassLoader classLoader;
        private final String className;
        private int foundCount = 0;

        public MethodScanner(ClassLoader classLoader, String className) {
            super(org.objectweb.asm.Opcodes.ASM9);
            this.classLoader = classLoader;
            this.className = className;
        }

        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
            CosmicLambdaDetector.LambdaAnnotationInfo info =
                    CosmicLambdaDetector.detectInBytecode(classLoader, className, name, descriptor);
            if (info != null) {
                String key = className + "." + name + descriptor;
                cache.put(key, info);
                foundCount++;
                System.out.println("      [@CosmicLambda] " + className.replace('/', '.') + "." + name + "()");
            }
            return null;
        }

        public int getFoundCount() { return foundCount; }
    }
}

package com.emir.analyzer.heuristics;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import java.io.InputStream;

/**
 * Heurística 3: Análisis de contexto de la clase.
 * Subsistema: Persistence Heuristic Engine.
 * Movida de com.emir.analyzer.detection.heuristics. Lógica IDÉNTICA.
 */
public class ClassContextAnalyzer {

    public static double scoreServiceClass(String owner) {
        String lower = owner.toLowerCase();
        if (lower.contains("/service/") || lower.contains("/services/") ||
                lower.contains("/repository/") || lower.contains("/repositories/") ||
                lower.contains("/dao/") || lower.contains("/persistence/") ||
                lower.contains("/data/")) return 1.0;
        if (lower.endsWith("service") || lower.endsWith("repository") ||
                lower.endsWith("dao")) return 1.0;
        if (lower.contains("/rest/") || lower.contains("/api/") ||
                lower.contains("/client/") || lower.contains("/remote/") ||
                lower.contains("/integration/")) return 0.7;
        if (lower.endsWith("client") || lower.endsWith("gateway") ||
                lower.endsWith("proxy") || lower.contains("operations")) return 0.7;
        if (isEntityClass(owner)) return 0.0;
        return 0.3;
    }

    public static boolean isEntityClass(String owner) {
        String lower = owner.toLowerCase();
        boolean isEntityByPackage = lower.contains("/entity/") || lower.contains("/entities/") ||
                lower.contains("/model/") || lower.contains("/models/") ||
                lower.contains("/domain/") || lower.contains("/dto/") ||
                lower.contains("/dtos/") || lower.contains("/message/") ||
                lower.contains("/pojo/") || lower.contains("/bean/") ||
                lower.contains("/vo/") || lower.contains("/data/object/");
        if (isEntityByPackage) return true;
        return hasEntityAnnotation(owner);
    }

    private static boolean hasEntityAnnotation(String owner) {
        try {
            String classResourcePath = owner + ".class";
            InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(classResourcePath);
            if (is == null) return false;
            ClassReader reader = new ClassReader(is);
            EntityAnnotationDetector detector = new EntityAnnotationDetector();
            reader.accept(detector, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();
            boolean hasEntity = detector.hasEntityAnnotation();
            if (hasEntity) {
                System.out.println("      [ENTITY] Detectada anotación @Entity en: " + owner.replace('/', '.'));
            }
            return hasEntity;
        } catch (Exception e) { return false; }
    }

    private static class EntityAnnotationDetector extends ClassVisitor {
        private boolean hasEntity = false;
        public EntityAnnotationDetector() { super(Opcodes.ASM9); }
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals("Ljavax/persistence/Entity;") ||
                    descriptor.equals("Ljakarta/persistence/Entity;")) {
                hasEntity = true;
                System.out.println("         [ASM] Encontrada anotación: " + descriptor);
            }
            return null;
        }
        public boolean hasEntityAnnotation() { return hasEntity; }
    }
}

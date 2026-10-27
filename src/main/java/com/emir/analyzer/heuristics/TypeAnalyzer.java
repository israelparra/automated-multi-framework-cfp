package com.emir.analyzer.heuristics;

import org.objectweb.asm.Type;

/**
 * Heurística 4: Análisis de tipos involucrados.
 * Subsistema: Persistence Heuristic Engine.
 * Movida de com.emir.analyzer.detection.heuristics. Lógica IDÉNTICA.
 */
public class TypeAnalyzer {

    public static double scoreEntityInvolved(String descriptor) {
        try {
            Type methodType = Type.getMethodType(descriptor);
            Type returnType = methodType.getReturnType();
            Type[] params = methodType.getArgumentTypes();
            if (isLikelyEntity(returnType)) return 1.0;
            for (Type param : params) { if (isLikelyEntity(param)) return 1.0; }
            return 0.0;
        } catch (Exception e) { return 0.0; }
    }

    public static boolean isLikelyEntity(Type type) {
        String className = type.getClassName();
        if (isPrimitive(className)) return false;
        if (className.startsWith("java.lang.") || className.startsWith("java.util.") ||
                className.startsWith("java.time.") || className.startsWith("java.math.") ||
                className.startsWith("java.io.") || className.startsWith("java.net.")) return false;
        String lower = className.toLowerCase();
        return lower.contains(".entity.") || lower.contains(".entities.") ||
                lower.contains(".model.") || lower.contains(".models.") ||
                lower.contains(".domain.") || lower.contains(".dto.") ||
                lower.contains(".dtos.") || lower.endsWith("entity") ||
                lower.endsWith("dto") || lower.endsWith("model") || lower.endsWith("vo");
    }

    private static boolean isPrimitive(String className) {
        return className.equals("int") || className.equals("long") || className.equals("boolean") ||
                className.equals("double") || className.equals("float") || className.equals("short") ||
                className.equals("byte") || className.equals("char") || className.equals("void") ||
                className.equals("java.lang.Integer") || className.equals("java.lang.Long") ||
                className.equals("java.lang.Boolean") || className.equals("java.lang.Double") ||
                className.equals("java.lang.Float") || className.equals("java.lang.Short") ||
                className.equals("java.lang.Byte") || className.equals("java.lang.Character") ||
                className.equals("java.lang.String") || className.equals("java.lang.Void");
    }
}

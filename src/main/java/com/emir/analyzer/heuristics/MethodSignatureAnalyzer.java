package com.emir.analyzer.heuristics;

import org.objectweb.asm.Type;

/**
 * Heurística 2: Análisis de firma del método.
 * Subsistema: Persistence Heuristic Engine.
 * Movida de com.emir.analyzer.detection.heuristics. Lógica IDÉNTICA.
 */
public class MethodSignatureAnalyzer {

    public static double scoreRead(String methodName, String descriptor) {
        try {
            Type methodType = Type.getMethodType(descriptor);
            Type[] params = methodType.getArgumentTypes();
            Type returnType = methodType.getReturnType();
            double score = 0.0;
            if (hasIdParameter(params)) score += 0.5;
            if (returnsEntityOrCollection(returnType)) score += 0.5;
            if (score >= 1.0) return 1.0;
            return score;
        } catch (Exception e) { return 0.0; }
    }

    public static double scoreWrite(String methodName, String descriptor) {
        try {
            Type methodType = Type.getMethodType(descriptor);
            Type[] params = methodType.getArgumentTypes();
            double score = 0.0;
            if (hasEntityParameter(params)) score += 0.7;
            if (params.length >= 2) score += 0.3;
            return Math.min(score, 1.0);
        } catch (Exception e) { return 0.0; }
    }

    private static boolean hasIdParameter(Type[] params) {
        for (Type param : params) {
            String cn = param.getClassName();
            if (cn.equals("java.lang.Long") || cn.equals("java.lang.Integer") ||
                    cn.equals("long") || cn.equals("int") || cn.equals("java.lang.String") ||
                    cn.equals("java.util.UUID")) return true;
        }
        return false;
    }

    private static boolean hasEntityParameter(Type[] params) {
        for (Type param : params) {
            if (TypeAnalyzer.isLikelyEntity(param)) return true;
        }
        return false;
    }

    private static boolean returnsEntityOrCollection(Type returnType) {
        String cn = returnType.getClassName();
        if (TypeAnalyzer.isLikelyEntity(returnType)) return true;
        if (cn.startsWith("java.util.List") || cn.startsWith("java.util.Set") ||
                cn.startsWith("java.util.Collection") || cn.startsWith("java.util.ArrayList") ||
                cn.startsWith("java.util.HashSet")) return true;
        if (cn.startsWith("java.util.Optional") || cn.startsWith("java.util.stream.Stream")) return true;
        return false;
    }
}

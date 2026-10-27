package com.emir.analyzer.heuristics;

import java.util.Set;

/**
 * Motor de inferencia multi-heurística para detectar operaciones de persistencia.
 * Subsistema: Persistence Heuristic Engine.
 *
 * Implementa IPersistenceHeuristicEngine.
 * Lógica funcional IDÉNTICA al original en com.emir.analyzer.detection.
 */
public class PersistenceHeuristicEngine implements IPersistenceHeuristicEngine {

    public enum PersistenceType {
        READ,
        WRITE,
        NONE
    }

    private final Set<String> projectPackages;

    public PersistenceHeuristicEngine(Set<String> projectPackages) {
        this.projectPackages = projectPackages;
        HeuristicConfig.validate();
    }

    @Override
    public PersistenceType classifyPersistence(String owner, String methodName, String descriptor) {
        return analyze(owner, methodName, descriptor);
    }

    public PersistenceType analyze(String owner, String methodName, String descriptor) {
        if (!isProjectClass(owner)) {
            if (isStandardLibrary(owner)) return PersistenceType.NONE;
            if (isEntityGetterSetter(owner, methodName)) return PersistenceType.NONE;

            PersistenceType reactiveType = detectReactiveFrameworks(owner, methodName, descriptor);
            if (reactiveType != PersistenceType.NONE) return reactiveType;

            double readScore = calculateReadScore(owner, methodName, descriptor);
            double writeScore = calculateWriteScore(owner, methodName, descriptor);

            if (readScore >= HeuristicConfig.THRESHOLD_READ && readScore > writeScore) return PersistenceType.READ;
            if (writeScore >= HeuristicConfig.THRESHOLD_WRITE) return PersistenceType.WRITE;
            return PersistenceType.NONE;
        }

        if (ClassContextAnalyzer.isEntityClass(owner)) return detectEntityCRUDOperation(methodName);
        return PersistenceType.NONE;
    }

    private PersistenceType detectEntityCRUDOperation(String methodName) {
        String methodLower = methodName.toLowerCase();

        if (methodLower.equals("findall") || methodLower.equals("listall") ||
                methodLower.equals("findbyid") || methodLower.equals("find") ||
                methodLower.equals("list") || methodLower.equals("get") ||
                methodLower.equals("getall") || methodLower.equals("count")) {
            return PersistenceType.READ;
        }

        if (methodLower.equals("persist") || methodLower.equals("save") ||
                methodLower.equals("insert") || methodLower.equals("update") ||
                methodLower.equals("delete") || methodLower.equals("remove") ||
                methodLower.equals("merge") || methodLower.equals("flush")) {
            return PersistenceType.WRITE;
        }

        return PersistenceType.NONE;
    }

    private PersistenceType detectReactiveFrameworks(String owner, String methodName, String descriptor) {
        boolean isReactiveDBClient = false;
        String frameworkName = null;

        if (owner.contains("io/vertx") && (owner.contains("pgclient") ||
                owner.contains("sqlclient") || owner.contains("mysqlclient"))) {
            isReactiveDBClient = true;
            frameworkName = "Vert.x";
        }
        if (owner.contains("io/quarkus/panache")) {
            isReactiveDBClient = true;
            frameworkName = "Panache";
        }
        if (owner.contains("io/r2dbc") || (owner.contains("org/springframework/data/r2dbc") &&
                owner.contains("DatabaseClient"))) {
            isReactiveDBClient = true;
            frameworkName = "R2DBC";
        }

        if (!isReactiveDBClient) return PersistenceType.NONE;

        String methodLower = methodName.toLowerCase();

        if (methodLower.equals("query") || methodLower.equals("find") ||
                methodLower.equals("findall") || methodLower.equals("findbyid") ||
                methodLower.equals("list") || methodLower.equals("listall") ||
                methodLower.equals("select") || methodLower.equals("get") ||
                methodLower.equals("getall") || methodLower.equals("count")) {
            return PersistenceType.READ;
        }

        if (methodLower.equals("persist") || methodLower.equals("save") ||
                methodLower.equals("insert") || methodLower.equals("update") ||
                methodLower.equals("delete") || methodLower.equals("remove") ||
                methodLower.equals("create") || methodLower.equals("merge") ||
                methodLower.equals("executebatch") || methodLower.equals("executeupdate")) {
            return PersistenceType.WRITE;
        }

        if (methodLower.equals("preparedquery") || methodLower.contains("prepared") ||
                methodLower.equals("execute")) {
            if (descriptor != null && descriptor.endsWith(")V")) return PersistenceType.WRITE;
            return PersistenceType.READ;
        }

        return PersistenceType.NONE;
    }

    private double calculateReadScore(String owner, String methodName, String descriptor) {
        double score = 0.0;
        score += HeuristicConfig.getWeightNaming() * NamingConventionAnalyzer.scoreRead(methodName);
        score += HeuristicConfig.getWeightSignature() * MethodSignatureAnalyzer.scoreRead(methodName, descriptor);
        score += HeuristicConfig.getWeightContext() * ClassContextAnalyzer.scoreServiceClass(owner);
        score += HeuristicConfig.getWeightTypeAnalysis() * TypeAnalyzer.scoreEntityInvolved(descriptor);
        return score;
    }

    private double calculateWriteScore(String owner, String methodName, String descriptor) {
        double score = 0.0;
        score += HeuristicConfig.getWeightNaming() * NamingConventionAnalyzer.scoreWrite(methodName);
        score += HeuristicConfig.getWeightSignature() * MethodSignatureAnalyzer.scoreWrite(methodName, descriptor);
        score += HeuristicConfig.getWeightContext() * ClassContextAnalyzer.scoreServiceClass(owner);
        score += HeuristicConfig.getWeightTypeAnalysis() * TypeAnalyzer.scoreEntityInvolved(descriptor);
        return score;
    }

    private boolean isProjectClass(String owner) {
        for (String pkg : projectPackages) {
            if (owner.startsWith(pkg)) return true;
        }
        return false;
    }

    private boolean isStandardLibrary(String owner) {
        return owner.startsWith("java/") || owner.startsWith("javax/") ||
                owner.startsWith("jakarta/") || owner.startsWith("sun/") ||
                owner.startsWith("jdk/");
    }

    private boolean isEntityGetterSetter(String owner, String methodName) {
        String lower = methodName.toLowerCase();
        boolean isGetterSetter = (lower.startsWith("get") || lower.startsWith("set") ||
                lower.startsWith("is")) && methodName.length() > 3;
        if (!isGetterSetter) return false;
        return ClassContextAnalyzer.isEntityClass(owner);
    }
}

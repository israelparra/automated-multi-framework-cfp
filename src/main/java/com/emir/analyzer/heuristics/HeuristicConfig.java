package com.emir.analyzer.heuristics;

import com.emir.analyzer.config.HeuristicEngineConfig;

/**
 * Wrapper de configuración del motor heurístico.
 * Subsistema: Persistence Heuristic Engine.
 *
 * Movido de com.emir.analyzer.detection.config.
 * Lógica funcional IDÉNTICA.
 */
public class HeuristicConfig {

    private static HeuristicEngineConfig engineConfig;

    public static void initialize(HeuristicEngineConfig config) {
        engineConfig = config;
        validate();
    }

    public static double getWeightNaming() {
        return engineConfig != null ? engineConfig.getWeights().getNaming() : 0.40;
    }

    public static double getWeightSignature() {
        return engineConfig != null ? engineConfig.getWeights().getSignature() : 0.30;
    }

    public static double getWeightContext() {
        return engineConfig != null ? engineConfig.getWeights().getContext() : 0.20;
    }

    public static double getWeightTypeAnalysis() {
        return engineConfig != null ? engineConfig.getWeights().getTypeAnalysis() : 0.10;
    }

    public static double getThresholdRead() {
        return engineConfig != null ? engineConfig.getThresholds().getRead() : 0.50;
    }

    public static double getThresholdWrite() {
        return engineConfig != null ? engineConfig.getThresholds().getWrite() : 0.50;
    }

    public static boolean isDebugMode() {
        return engineConfig != null && engineConfig.isDebugMode();
    }

    public static final double WEIGHT_NAMING = 0.40;
    public static final double WEIGHT_SIGNATURE = 0.30;
    public static final double WEIGHT_CONTEXT = 0.20;
    public static final double WEIGHT_TYPE_ANALYSIS = 0.10;
    public static final double THRESHOLD_READ = 0.50;
    public static final double THRESHOLD_WRITE = 0.50;
    public static final boolean DEBUG_MODE = false;

    private HeuristicConfig() {}

    public static void validate() {
        if (engineConfig == null) {
            System.out.println("⚠️ [HeuristicConfig] No se cargó configuración, usando valores por defecto");
            return;
        }
        double totalWeight = getWeightNaming() + getWeightSignature() +
                getWeightContext() + getWeightTypeAnalysis();
        if (Math.abs(totalWeight - 1.0) > 0.001) {
            throw new IllegalStateException("Los pesos deben sumar 1.0. Total actual: " + totalWeight);
        }
        if (getThresholdRead() < 0.0 || getThresholdRead() > 1.0) {
            throw new IllegalStateException("THRESHOLD_READ debe estar entre 0.0 y 1.0");
        }
        if (getThresholdWrite() < 0.0 || getThresholdWrite() > 1.0) {
            throw new IllegalStateException("THRESHOLD_WRITE debe estar entre 0.0 y 1.0");
        }
    }
}

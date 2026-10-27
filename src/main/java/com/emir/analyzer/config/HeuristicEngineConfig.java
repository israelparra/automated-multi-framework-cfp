package com.emir.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Configuración del motor heurístico de detección de persistencia.
 */
@Data
public class HeuristicEngineConfig {

    @JsonProperty("weights")
    private HeuristicWeights weights = new HeuristicWeights();

    @JsonProperty("thresholds")
    private HeuristicThresholds thresholds = new HeuristicThresholds();

    @JsonProperty("debug_mode")
    private boolean debugMode = false;

    /**
     * Valida que los pesos sumen 1.0.
     */
    public void validate() {
        double total = weights.sum();
        if (Math.abs(total - 1.0) > 0.001) {
            throw new IllegalStateException(
                    "Los pesos del motor heurístico deben sumar 1.0. Total actual: " + total
            );
        }

        if (thresholds.getRead() < 0.0 || thresholds.getRead() > 1.0) {
            throw new IllegalStateException("read_threshold debe estar entre 0.0 y 1.0");
        }

        if (thresholds.getWrite() < 0.0 || thresholds.getWrite() > 1.0) {
            throw new IllegalStateException("write_threshold debe estar entre 0.0 y 1.0");
        }
    }

    @Data
    public static class HeuristicWeights {
        @JsonProperty("naming")
        private double naming = 0.40;

        @JsonProperty("signature")
        private double signature = 0.30;

        @JsonProperty("context")
        private double context = 0.20;

        @JsonProperty("type_analysis")
        private double typeAnalysis = 0.10;

        public double sum() {
            return naming + signature + context + typeAnalysis;
        }
    }

    @Data
    public static class HeuristicThresholds {
        @JsonProperty("read")
        private double read = 0.50;

        @JsonProperty("write")
        private double write = 0.50;
    }
}
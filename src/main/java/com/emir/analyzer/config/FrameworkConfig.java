package com.emir.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Configuración completa de un framework específico.
 */
@Data
public class FrameworkConfig {

    @JsonProperty("annotations")
    private FrameworkAnnotations annotations = new FrameworkAnnotations();

    @JsonProperty("infrastructure")
    private FrameworkInfrastructure infrastructure = new FrameworkInfrastructure();

    @JsonProperty("component_scanner")
    private ComponentScannerConfig componentScanner = new ComponentScannerConfig();
}
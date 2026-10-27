package com.emir.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración del scanner de componentes.
 */
@Data
public class ComponentScannerConfig {

    @JsonProperty("scan_annotations")
    private List<String> scanAnnotations = new ArrayList<>();
}
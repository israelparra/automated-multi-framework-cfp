package com.emir.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de infraestructura de un framework.
 */
@Data
public class FrameworkInfrastructure {

    @JsonProperty("dependencies")
    private List<String> dependencies = new ArrayList<>();

    @JsonProperty("annotations")
    private List<String> annotations = new ArrayList<>();
}
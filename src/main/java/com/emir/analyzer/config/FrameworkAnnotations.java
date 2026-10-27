package com.emir.analyzer.config;

import com.emir.analyzer.annotations.CosmicLambda;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Anotaciones específicas de un framework.
 */
@Data
public class FrameworkAnnotations {

    @JsonProperty("rest_controller")
    private String restController = "";

    @JsonProperty("http_mappings")
    private Set<String> httpMappings = new HashSet<>();

    @JsonProperty("entity")
    private Set<String> entity = new HashSet<>();

    @JsonProperty("repository")
    private Set<String> repository = new HashSet<>();

    @JsonProperty("main_application")
    private String mainApplication = "";

    @JsonProperty("inject")
    private String inject = "";

    @JsonProperty("transactional")
    private String transactional = "";

}
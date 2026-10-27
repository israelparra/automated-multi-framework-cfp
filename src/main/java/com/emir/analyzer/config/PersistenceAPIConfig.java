package com.emir.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de una API de persistencia (JPA, Spring Data, etc.).
 */
@Data
public class PersistenceAPIConfig {

    @JsonProperty("class_name")
    private String className = "";

    @JsonProperty("write_methods")
    private List<String> writeMethods = new ArrayList<>();

    @JsonProperty("read_methods")
    private List<String> readMethods = new ArrayList<>();

    @JsonProperty("repository_base")
    private String repositoryBase = "";

    @JsonProperty("read_prefixes")
    private List<String> readPrefixes = new ArrayList<>();

    @JsonProperty("write_prefixes")
    private List<String> writePrefixes = new ArrayList<>();
}
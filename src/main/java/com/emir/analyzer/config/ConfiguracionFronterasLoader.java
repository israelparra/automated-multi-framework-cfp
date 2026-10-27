package com.emir.analyzer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Cargador de configuración multi-archivo con sistema de herencia.
 *
 * VERSIÓN 3.0 - SIN SOPORTE PARA fronteras.json
 *
 * ESTRATEGIA DE BÚSQUEDA (en orden):
 * 1. config/frameworks/{framework}-configuration.json (PERSONALIZADO)
 * 2. config/frameworks/{framework}-fronteras.json (estándar)
 * 3. ERROR si no se encuentra
 *
 * @author Sistema de Análisis COSMIC v3.0
 */
public class ConfiguracionFronterasLoader {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String CONFIG_DIR = "config";
    private static final String FRAMEWORKS_DIR = CONFIG_DIR + "/frameworks";
    private static final String COMMON_DIR = CONFIG_DIR + "/common";

    /**
     * Carga la configuración para un framework específico.
     *
     * @param frameworkName Nombre del framework (spring_boot, jakarta_ee)
     * @return Configuración completa con herencia resuelta
     * @throws RuntimeException si no se encuentra la configuración
     */
    public static ConfiguracionFronteras loadForFramework(String frameworkName) {
        ConfiguracionFronteras config = tryLoadFrameworkFile(frameworkName);
        if (config != null) return config;

        // Si no se encuentra, lanzar error explicativo
        throw new RuntimeException(
                "❌ No se encontró configuración para framework: " + frameworkName + "\n" +
                        "   Archivos esperados (en orden de búsqueda):\n" +
                        "   1. " + FRAMEWORKS_DIR + "/" + frameworkName.replace("_", "") + "-configuration.json\n" +
                        "   2. " + FRAMEWORKS_DIR + "/" + frameworkName + "-configuration.json\n" +
                        "   3. " + FRAMEWORKS_DIR + "/" + frameworkName + "-fronteras.json\n\n" +
                        "   📝 Solución: Crear el archivo de configuración correspondiente en config/frameworks/"
        );
    }

    /**
     * Intenta cargar el archivo de configuración específico del framework.
     * Resuelve herencia si el archivo tiene "extends".
     */
    private static ConfiguracionFronteras tryLoadFrameworkFile(String frameworkName) {
        // Posibles ubicaciones del archivo (con soporte para nombres personalizados)
        String[] possiblePaths = {
                // FORMATO PERSONALIZADO (sin guión bajo: springboot, jakartaee)
                FRAMEWORKS_DIR + "/" + frameworkName.replace("_", "") + "-configuration.json",

                // FORMATO ESTÁNDAR (con guión bajo: spring_boot, jakarta_ee)
                FRAMEWORKS_DIR + "/" + frameworkName + "-configuration.json",

                // FORMATO ORIGINAL (fallback)
                FRAMEWORKS_DIR + "/" + frameworkName + "-fronteras.json"
        };

        for (String path : possiblePaths) {
            File file = new File(path);

            if (file.exists()) {
                try {
                    JsonNode rootNode = mapper.readTree(file);

                    if (rootNode.has("extends")) {
                        String parentPath = rootNode.get("extends").asText();
                        JsonNode parentNode = loadParentConfig(parentPath, file.getParentFile());

                        JsonNode mergedNode = mergeConfigs(parentNode, rootNode);
                        return parseConfiguration(mergedNode, frameworkName);
                    } else {
                        return parseConfiguration(rootNode, frameworkName);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Error cargando configuración: " + path, e);
                }
            }
        }

        return null;
    }

    /**
     * Carga un archivo padre desde el directorio common o relativo.
     */
    private static JsonNode loadParentConfig(String parentPath, File currentDir) throws IOException {
        // Lista de posibles ubicaciones del archivo padre
        File[] possibleParentFiles = {
                new File(currentDir, parentPath),                              // Relativo al archivo hijo
                new File(COMMON_DIR + "/common-configuration.json"),           // PERSONALIZADO
                new File(COMMON_DIR + "/common-fronteras.json"),               // ESTÁNDAR
                new File(COMMON_DIR, parentPath),                              // Ruta en common/ con nombre del extends
                new File(parentPath)                                            // Raíz del proyecto
        };

        for (File parentFile : possibleParentFiles) {
            if (parentFile.exists()) {
                return mapper.readTree(parentFile);
            }
        }

        throw new IOException(
                "❌ No se encontró archivo padre: " + parentPath + "\n" +
                        "   Ubicaciones buscadas:\n" +
                        "   - " + new File(currentDir, parentPath).getAbsolutePath() + "\n" +
                        "   - " + new File(COMMON_DIR + "/common-configuration.json").getAbsolutePath() + "\n" +
                        "   - " + new File(COMMON_DIR + "/common-fronteras.json").getAbsolutePath()
        );
    }

    /**
     * Fusiona dos configuraciones (padre + hijo).
     * El hijo sobrescribe valores del padre.
     */
    private static JsonNode mergeConfigs(JsonNode parent, JsonNode child) {
        ObjectNode merged = parent.deepCopy();

        // Iterar sobre todos los campos del hijo
        Iterator<Map.Entry<String, JsonNode>> fields = child.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            // Ignorar metadata
            if (key.equals("extends") || key.equals("comment") ||
                    key.equals("version") || key.equals("framework")) {
                continue;
            }

            // Si el campo existe en el padre y ambos son objetos, fusionar recursivamente
            if (merged.has(key) && merged.get(key).isObject() && value.isObject()) {
                JsonNode mergedChild = mergeConfigs(merged.get(key), value);
                merged.set(key, mergedChild);
            }
            // Si el campo es un array, CONCATENAR (no reemplazar)
            else if (merged.has(key) && merged.get(key).isArray() && value.isArray()) {
                // Para arrays, mantener valores del padre y agregar los del hijo
                Set<String> combinedValues = new HashSet<>();

                // Agregar valores del padre
                merged.get(key).forEach(n -> combinedValues.add(n.asText()));

                // Agregar valores del hijo
                value.forEach(n -> combinedValues.add(n.asText()));

                // Crear nuevo array combinado
                com.fasterxml.jackson.databind.node.ArrayNode combinedArray =
                        mapper.createArrayNode();
                combinedValues.forEach(combinedArray::add);

                merged.set(key, combinedArray);
            }
            else {
                // Sobrescribir o agregar
                merged.set(key, value);
            }
        }

        return merged;
    }

    /**
     * Convierte un JsonNode fusionado en ConfiguracionFronteras.
     */
    private static ConfiguracionFronteras parseConfiguration(JsonNode node, String frameworkName) throws IOException {
        ConfiguracionFronteras config = new ConfiguracionFronteras();

        // PASO 1: Cargar frameworks si existe
        if (node.has("frameworks")) {
            JsonNode frameworksNode = node.get("frameworks");

            Iterator<Map.Entry<String, JsonNode>> frameworks = frameworksNode.fields();
            while (frameworks.hasNext()) {
                Map.Entry<String, JsonNode> entry = frameworks.next();
                String fwName = entry.getKey();

                FrameworkConfig frameworkConfig = mapper.treeToValue(entry.getValue(), FrameworkConfig.class);
                config.getFrameworks().put(fwName, frameworkConfig);
            }
        }

        // PASO 2: Cargar persistence_apis si existe
        if (node.has("persistence_apis")) {
            JsonNode persistenceNode = node.get("persistence_apis");

            Iterator<Map.Entry<String, JsonNode>> apis = persistenceNode.fields();
            while (apis.hasNext()) {
                Map.Entry<String, JsonNode> entry = apis.next();

                PersistenceAPIConfig apiConfig = mapper.treeToValue(entry.getValue(), PersistenceAPIConfig.class);
                config.getPersistenceApis().put(entry.getKey(), apiConfig);
            }
        }

        // PASO 3: Cargar heuristic_engine si existe
        if (node.has("heuristic_engine")) {
            HeuristicEngineConfig heuristicConfig = mapper.treeToValue(
                    node.get("heuristic_engine"),
                    HeuristicEngineConfig.class
            );
            config.setHeuristicEngine(heuristicConfig);
        }

        // PASO 4: Cargar fronteras generales (arrays simples)
        if (node.has("api_fronteras")) {
            node.get("api_fronteras").forEach(n -> config.getApiFronteras().add(n.asText()));
        }

        if (node.has("anotaciones_api_fronteras")) {
            node.get("anotaciones_api_fronteras").forEach(n -> config.getAnotacionesApiFronteras().add(n.asText()));
        }

        if (node.has("mensajeria_fronteras")) {
            node.get("mensajeria_fronteras").forEach(n -> config.getMensajeriaFronteras().add(n.asText()));
        }

        if (node.has("archivos_write_fronteras")) {
            node.get("archivos_write_fronteras").forEach(n -> config.getArchivosWriteFronteras().add(n.asText()));
        }

        if (node.has("archivos_read_fronteras")) {
            node.get("archivos_read_fronteras").forEach(n -> config.getArchivosReadFronteras().add(n.asText()));
        }

        if (node.has("nomenclatura_read_prefijos")) {
            node.get("nomenclatura_read_prefijos").forEach(n -> config.getNomenclaturaReadPrefijos().add(n.asText()));
        }

        if (node.has("nomenclatura_write_prefijos")) {
            node.get("nomenclatura_write_prefijos").forEach(n -> config.getNomenclaturaWritePrefijos().add(n.asText()));
        }

        // PASO 5: Cargar jakarta_persistence (legacy)
        if (node.has("jakarta_persistence")) {
            JsonNode jakartaNode = node.get("jakarta_persistence");
            Iterator<Map.Entry<String, JsonNode>> fields = jakartaNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                config.getJakartaPersistence().put(entry.getKey(), entry.getValue().asText());
            }
        }

        return config;
    }
}
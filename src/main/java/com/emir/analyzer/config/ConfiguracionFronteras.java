package com.emir.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Configuración completa cargada desde fronteras.json.
 *
 * VERSIÓN 2.0: Ahora incluye configuración de frameworks, persistence APIs y heurísticas.
 *
 * RETROCOMPATIBILIDAD: Mantiene todos los campos de la versión anterior.
 */
@Data
public class ConfiguracionFronteras {

    // ========== CONFIGURACIÓN DE FRAMEWORKS ==========

    @JsonProperty("frameworks")
    private Map<String, FrameworkConfig> frameworks = new HashMap<>();

    // ========== CONFIGURACIÓN DE PERSISTENCE APIs ==========

    @JsonProperty("persistence_apis")
    private Map<String, PersistenceAPIConfig> persistenceApis = new HashMap<>();

    // ========== CONFIGURACIÓN DEL MOTOR HEURÍSTICO ==========

    @JsonProperty("heuristic_engine")
    private HeuristicEngineConfig heuristicEngine;

    // ========== FRONTERAS GENERALES (Retrocompatibilidad) ==========

    @JsonProperty("api_fronteras")
    private Set<String> apiFronteras = new HashSet<>();

    @JsonProperty("anotaciones_api_fronteras")
    private Set<String> anotacionesApiFronteras = new HashSet<>();

    @JsonProperty("mensajeria_fronteras")
    private Set<String> mensajeriaFronteras = new HashSet<>();

    @JsonProperty("archivos_write_fronteras")
    private Set<String> archivosWriteFronteras = new HashSet<>();

    @JsonProperty("archivos_read_fronteras")
    private Set<String> archivosReadFronteras = new HashSet<>();

    @JsonProperty("nomenclatura_read_prefijos")
    private Set<String> nomenclaturaReadPrefijos = new HashSet<>();

    @JsonProperty("nomenclatura_write_prefijos")
    private Set<String> nomenclaturaWritePrefijos = new HashSet<>();

    // ========== JAKARTA PERSISTENCE (Retrocompatibilidad) ==========

    @JsonProperty("jakarta_persistence")
    private Map<String, String> jakartaPersistence = new HashMap<>();

    // ========== CONSTRUCTORES ==========

    /**
     * Constructor por defecto con valores iniciales.
     */
    public ConfiguracionFronteras() {
        initializeDefaults();
    }

    /**
     * Inicializa valores por defecto para retrocompatibilidad.
     * SOLO se usa si fronteras.json no existe o está incompleto.
     */
    private void initializeDefaults() {
        // API Fronteras
        apiFronteras.add("org/springframework/web/client/RestTemplate");
        apiFronteras.add("org/springframework/web/reactive/function/client/WebClient");
        apiFronteras.add("jakarta/ws/rs/client/Client");
        apiFronteras.add("jakarta/ws/rs/client/WebTarget");
        apiFronteras.add("org/apache/http/client/HttpClient");

        // Anotaciones de API externa (FEIGN)
        anotacionesApiFronteras.add("org/springframework/cloud/openfeign/FeignClient");

        // Mensajería
        mensajeriaFronteras.add("org/springframework/kafka/core/KafkaTemplate");
        mensajeriaFronteras.add("org/springframework/amqp/rabbit/core/RabbitTemplate");
        mensajeriaFronteras.add("org/springframework/jms/core/JmsTemplate");
        mensajeriaFronteras.add("jakarta/jms/MessageProducer");
        mensajeriaFronteras.add("jakarta/jms/QueueSender");

        // Archivos Write
        archivosWriteFronteras.add("java/nio/file/Files.write");
        archivosWriteFronteras.add("java/io/FileOutputStream.write");
        archivosWriteFronteras.add("java/io/FileWriter.write");

        // Archivos Read
        archivosReadFronteras.add("java/nio/file/Files.readAllBytes");
        archivosReadFronteras.add("java/io/FileInputStream.read");

        // Nomenclatura READ
        nomenclaturaReadPrefijos.addAll(Arrays.asList(
                "find", "get", "read", "search", "stream", "query",
                "count", "exists", "is", "buscar", "obtener", "consultar", "leer"
        ));

        // Nomenclatura WRITE
        nomenclaturaWritePrefijos.addAll(Arrays.asList(
                "save", "delete", "remove", "persist", "merge", "update",
                "add", "create", "guardar", "borrar", "eliminar", "actualizar", "crear", "modificar"
        ));

        // Jakarta Persistence
        jakartaPersistence.put("entity_manager", "jakarta/persistence/EntityManager");
        jakartaPersistence.put("query", "jakarta/persistence/Query");
        jakartaPersistence.put("typed_query", "jakarta/persistence/TypedQuery");

        // Motor heurístico (valores por defecto)
        if (heuristicEngine == null) {
            heuristicEngine = new HeuristicEngineConfig();
        }
    }

    /**
     * Obtiene la configuración de un framework específico.
     *
     * @param frameworkName Nombre del framework (spring_boot, jakarta_ee)
     * @return Configuración del framework o null si no existe
     */
    public FrameworkConfig getFrameworkConfig(String frameworkName) {
        return frameworks.get(frameworkName);
    }

    /**
     * Valida que la configuración cargada sea coherente.
     */
    public void validate() {
        // Validar que existen configuraciones mínimas
        if (frameworks.isEmpty()) {
            System.out.println("⚠️ [Config] No se encontraron configuraciones de frameworks, usando valores por defecto");
        }

        // Validar motor heurístico
        if (heuristicEngine != null) {
            heuristicEngine.validate();
        }
    }

    /**
     * Carga la configuración desde un archivo.
     *
     * @param configPath Ruta al archivo fronteras.json
     * @return Configuración cargada
     */
    public static ConfiguracionFronteras loadFromFile(String configPath) {
        File configFile = new File(configPath);
        ObjectMapper mapper = new ObjectMapper();

        if (configFile.exists() && !configFile.isDirectory()) {
            try {
                ConfiguracionFronteras config = mapper.readValue(configFile, ConfiguracionFronteras.class);
                config.validate();
                System.out.println("✅ [Config] Cargada exitosamente desde: " + configFile.getAbsolutePath());
                return config;
            } catch (IOException e) {
                System.err.println("⚠️ [Config] Error al leer " + configPath + ": " + e.getMessage());
                System.err.println("   Usando configuración por defecto.");
            }
        } else {
            System.out.println("ℹ️ [Config] No se encontró " + configPath + ". Usando configuración por defecto.");
        }

        return new ConfiguracionFronteras();
    }
}
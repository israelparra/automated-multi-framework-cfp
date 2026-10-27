package com.emir.analyzer.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Modelo funcional COSMIC con movimientos clasificados y cálculo de CFP.
 * Subsistema: COSMIC Core Engine.
 * Movido de com.emir.analyzer. Lógica IDÉNTICA.
 *
 * El self-message exportReport() (msg 18) es interno de esta clase.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CosmicFunctionalModel {
    private List<CosmicDataMovement> entries;
    private List<CosmicDataMovement> exits;
    private List<CosmicDataMovement> reads;
    private List<CosmicDataMovement> writes;
    private UniversalComponentDetector universalComponentDetector;

    public CosmicFunctionalModel(UniversalComponentDetector detector) {
        this.universalComponentDetector = detector;
        this.entries = new LinkedList<>();
        this.exits = new LinkedList<>();
        this.reads = new LinkedList<>();
        this.writes = new LinkedList<>();
    }

    public void identifyMovements() {
        Map<CosmicDataMovementType, List<CosmicDataMovement>> movements =
                this.universalComponentDetector.getMovementTypeMap();
        this.entries = movements.get(CosmicDataMovementType.ENTRY);
        this.exits = movements.get(CosmicDataMovementType.EXIT);
        this.reads = movements.get(CosmicDataMovementType.READ);
        this.writes = movements.get(CosmicDataMovementType.WRITE);
    }

    public int calculateTotalCFP() {
        return entries.size() + exits.size() + reads.size() + writes.size();
    }

    @Data @AllArgsConstructor
    private static class JsonReport {
        private String projectName; private String analysisDate; private int totalCFP;
        private MovementSummary movements; private Map<String, Long> cfpByClass;
    }

    @Data @AllArgsConstructor
    private static class MovementSummary {
        private int entries; private int exits; private int reads; private int writes;
    }

    @Data @AllArgsConstructor
    private static class ServiceReport {
        private String name; private int totalCFP;
        private MovementSummary movements; private Map<String, Long> cfpByClass;
    }

    @Data @AllArgsConstructor
    private static class ConsolidatedReport {
        private String projectName; private String analysisDate;
        private int totalCFP; private List<ServiceReport> microservices;
    }

    /**
     * Exporta un único reporte JSON consolidado con todos los microservicios analizados.
     */
    public static void exportConsolidatedReport(String rootProjectName,
            Map<String, CosmicFunctionalModel> servicesMap, String outputDirectory) {
        System.out.println("\n💾 Exportando reporte consolidado JSON...");
        List<ServiceReport> services = new ArrayList<>();
        int grandTotal = 0;
        for (Map.Entry<String, CosmicFunctionalModel> entry : servicesMap.entrySet()) {
            CosmicFunctionalModel model = entry.getValue();
            MovementSummary summary = new MovementSummary(
                    model.entries.size(), model.exits.size(),
                    model.reads.size(), model.writes.size());
            services.add(new ServiceReport(entry.getKey(), model.calculateTotalCFP(),
                    summary, model.getClassStatisticsData()));
            grandTotal += model.calculateTotalCFP();
        }
        String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date());
        ConsolidatedReport report = new ConsolidatedReport(rootProjectName, dateStr, grandTotal, services);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        new File(outputDirectory).mkdirs();
        File outputFile = new File(outputDirectory, rootProjectName + ".json");
        try {
            mapper.writeValue(outputFile, report);
            System.out.println("✅ Reporte guardado en: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ Error al escribir el reporte JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void generateReport() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           COSMIC FUNCTION POINT ANALYSIS REPORT");
        System.out.println("=".repeat(60));
        System.out.println("\n📊 RESUMEN DE MOVIMIENTOS:");
        System.out.println("   Entry movements:  " + entries.size() + " CFP");
        System.out.println("   Exit movements:   " + exits.size() + " CFP");
        System.out.println("   Read movements:   " + reads.size() + " CFP");
        System.out.println("   Write movements:  " + writes.size() + " CFP");
        System.out.println("   " + "-".repeat(35));
        System.out.println("   TOTAL CFP:        " + calculateTotalCFP() + " CFP");
        System.out.println("\n📝 DETALLE POR TIPO DE MOVIMIENTO:");
        if (!entries.isEmpty()) { System.out.println("\n   ENTRY:"); entries.forEach(m -> System.out.println("      • " + m)); }
        if (!exits.isEmpty()) { System.out.println("\n   EXIT:"); exits.forEach(m -> System.out.println("      • " + m)); }
        if (!reads.isEmpty()) { System.out.println("\n   READ:"); reads.forEach(m -> System.out.println("      • " + m)); }
        if (!writes.isEmpty()) { System.out.println("\n   WRITE:"); writes.forEach(m -> System.out.println("      • " + m)); }
        System.out.println("\n" + "=".repeat(60));
        generateClassStatistics();
    }

    private void generateClassStatistics() {
        System.out.println("\n📈 ESTADÍSTICAS POR CLASE:");
        getClassStatisticsData().forEach((cn, count) -> System.out.println("   " + cn + ": " + count + " CFP"));
    }

    private Map<String, Long> getClassStatisticsData() {
        List<CosmicDataMovement> all = new LinkedList<>();
        all.addAll(entries); all.addAll(exits); all.addAll(reads); all.addAll(writes);
        return all.stream().collect(Collectors.groupingBy(CosmicDataMovement::getClassName, Collectors.counting()));
    }
}

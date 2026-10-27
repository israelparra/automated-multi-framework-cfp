package com.emir.analyzer.heuristics;

/**
 * Heurística 1: Análisis de convenciones de nomenclatura.
 * Subsistema: Persistence Heuristic Engine.
 * Movida de com.emir.analyzer.detection.heuristics.
 * Lógica funcional IDÉNTICA.
 */
public class NamingConventionAnalyzer {

    public static double scoreRead(String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.startsWith("find") || lower.startsWith("fetch") || lower.startsWith("load") ||
                lower.startsWith("retrieve") || lower.startsWith("query") || lower.startsWith("search") ||
                lower.startsWith("select") || lower.startsWith("read") || lower.startsWith("list")) {
            return 1.0;
        }
        if (lower.startsWith("buscar") || lower.startsWith("consultar") || lower.startsWith("listar") ||
                lower.startsWith("obtener") || lower.startsWith("leer")) {
            return 1.0;
        }
        if (lower.startsWith("get")) {
            if (lower.contains("entity") || lower.contains("entities") || lower.contains("all") ||
                    lower.contains("by") || lower.contains("with") || lower.contains("from") ||
                    lower.contains("where")) {
                return 0.7;
            }
        }
        if (lower.startsWith("count") || lower.startsWith("exists") || lower.startsWith("has") ||
                lower.startsWith("check")) {
            return 0.3;
        }
        if (lower.startsWith("contar") || lower.startsWith("existe") || lower.startsWith("tiene") ||
                lower.startsWith("verificar")) {
            return 0.3;
        }
        return 0.0;
    }

    public static double scoreWrite(String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.startsWith("save") || lower.startsWith("create") || lower.startsWith("insert") ||
                lower.startsWith("add") || lower.startsWith("update") || lower.startsWith("modify") ||
                lower.startsWith("edit") || lower.startsWith("delete") || lower.startsWith("remove") ||
                lower.startsWith("drop") || lower.startsWith("persist") || lower.startsWith("merge") ||
                lower.startsWith("store") || lower.startsWith("write")) {
            return 1.0;
        }
        if (lower.startsWith("guardar") || lower.startsWith("crear") || lower.startsWith("insertar") ||
                lower.startsWith("agregar") || lower.startsWith("actualizar") || lower.startsWith("modificar") ||
                lower.startsWith("eliminar") || lower.startsWith("borrar") || lower.startsWith("quitar")) {
            return 1.0;
        }
        if (lower.contains("forcreation") || lower.contains("forupdate") || lower.contains("fordeletion") ||
                lower.contains("forsave") || lower.contains("sendentity") || lower.contains("pushto") ||
                lower.contains("syncto")) {
            return 0.9;
        }
        if (lower.startsWith("set")) {
            if (lower.contains("entity") || lower.contains("entities") || lower.contains("todb") ||
                    lower.contains("todatabase") || lower.contains("persistent") || lower.contains("indb")) {
                return 0.5;
            }
        }
        if (lower.startsWith("put") || lower.startsWith("post")) {
            return 0.4;
        }
        return 0.0;
    }
}

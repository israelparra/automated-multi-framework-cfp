package com.emir.analyzer.framework.jakarta;

import com.emir.analyzer.framework.IComponentScanner;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ✅ MEJORADO: Ahora soporta Quarkus correctamente.
 *
 * Quarkus usa Jakarta EE APIs pero NO siempre tiene una clase principal
 * con @ApplicationPath explícita, especialmente en microservicios pequeños.
 */
public class JakartaComponentScanner implements IComponentScanner {

    @Override
    public Set<String> autoDiscoverPackages(Class<?> mainAppClass) {
        Set<String> packages = new HashSet<>();

        if (mainAppClass == null) {
            // ✅ QUARKUS: Sin clase principal, retornar vacío para activar FALLBACK
            System.out.println("   [Jakarta Scanner] No se encontró clase principal, delegando a FALLBACK");
            return packages;
        }

        // En Jakarta/Quarkus, el paquete base suele ser el de la clase ApplicationPath
        if (mainAppClass.getPackage() != null) {
            String basePkg = mainAppClass.getPackage().getName();
            packages.add(basePkg);
            System.out.println("   [Jakarta Scanner] Paquete base detectado: " + basePkg);
        }

        return packages.stream()
                .map(p -> p.replace('.', '/'))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> extractBasePackages(Class<?> clazz) {
        Set<String> packages = new HashSet<>();

        // Jakarta no tiene anotaciones de escaneo tan explícitas como Spring
        // Usamos el paquete de la clase como base
        if (clazz != null && clazz.getPackage() != null) {
            packages.add(clazz.getPackage().getName());
        }

        return packages;
    }

    @Override
    public String getDefaultPackage(Class<?> mainAppClass) {
        if (mainAppClass != null && mainAppClass.getPackage() != null) {
            return mainAppClass.getPackage().getName();
        }
        return "";
    }
}
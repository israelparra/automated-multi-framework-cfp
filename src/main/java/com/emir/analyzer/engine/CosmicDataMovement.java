package com.emir.analyzer.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.lang.reflect.Method;

/**
 * Representa un movimiento de datos individual según COSMIC.
 * Cada movimiento equivale a 1 CFP.
 * Subsistema: COSMIC Core Engine.
 * Movido de com.emir.analyzer.components. Lógica IDÉNTICA.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CosmicDataMovement {
    private String name;
    private CosmicDataMovementType type;
    private String dataGroup;
    private Method method;
    private String className;

    public CosmicDataMovement(String name, CosmicDataMovementType type, Method method, String className) {
        this.name = name;
        this.type = type;
        this.method = method;
        this.className = className;
        this.dataGroup = determineDataGroup(method);
    }

    public CosmicDataMovement(String name, CosmicDataMovementType type, String className) {
        this.name = name;
        this.type = type;
        this.method = null;
        this.className = className;
        this.dataGroup = "Unknown";
    }

    public int getCosmicFunctionPoints() { return 1; }

    private String determineDataGroup(Method method) {
        if (method == null) return "Unknown";
        try {
            if (method.getReturnType() != void.class) return method.getReturnType().getSimpleName();
            else if (method.getParameterCount() > 0) return method.getParameterTypes()[0].getSimpleName();
        } catch (Throwable t) { return "Unknown"; }
        return "Unknown";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - DataGroup: %s - Method: %s - Class: %s",
                type, name, dataGroup, method != null ? method.getName() : "N/A (bytecode)", className);
    }
}

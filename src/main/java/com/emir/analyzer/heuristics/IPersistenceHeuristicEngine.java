package com.emir.analyzer.heuristics;

/**
 * Interfaz del subsistema Persistence Heuristic Engine.
 *
 * Corresponde a los mensajes 14-15 del diagrama de secuencia:
 * classifyPersistence(owner, method, descriptor) → PersistenceType
 */
public interface IPersistenceHeuristicEngine {

    /**
     * Clasifica si una llamada a método representa persistencia.
     *
     * @param owner Nombre de la clase propietaria (formato interno)
     * @param methodName Nombre del método
     * @param descriptor Descriptor ASM del método
     * @return PersistenceType (READ, WRITE o NONE)
     */
    PersistenceHeuristicEngine.PersistenceType classifyPersistence(String owner, String methodName, String descriptor);
}

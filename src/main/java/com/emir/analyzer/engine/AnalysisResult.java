package com.emir.analyzer.engine;

import lombok.Data;

/**
 * Almacena el resultado del análisis de un método (counts de READ y WRITE).
 * Subsistema: COSMIC Core Engine.
 * Movido de com.emir.analyzer. Lógica IDÉNTICA.
 */
@Data
public class AnalysisResult {
    private int readCount = 0;
    private int writeCount = 0;

    public void addRead() { this.readCount++; }
    public void addWrite() { this.writeCount++; }

    public void add(AnalysisResult other) {
        this.readCount += other.getReadCount();
        this.writeCount += other.getWriteCount();
    }
}

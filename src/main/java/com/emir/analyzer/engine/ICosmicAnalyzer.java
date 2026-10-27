package com.emir.analyzer.engine;

import com.emir.analyzer.artifact.PreparedArtifact;
import com.emir.analyzer.config.ConfiguracionFronteras;
import com.emir.analyzer.framework.IFrameworkAdapter;

/**
 * Interfaz del subsistema COSMIC Core Engine.
 *
 * Corresponde a los mensajes 13-19 del diagrama de secuencia:
 * analyze(preparedArtifact, adapter, config) → CosmicFunctionalModel
 */
public interface ICosmicAnalyzer {

    /**
     * Ejecuta el análisis COSMIC completo sobre las clases preparadas.
     *
     * @param artifact Clases cargadas en memoria (PreparedArtifact)
     * @param adapter Adapter del framework (IFrameworkAdapter)
     * @param config Configuración de fronteras (ConfiguracionFronteras)
     * @return Modelo funcional COSMIC con movimientos clasificados
     */
    CosmicFunctionalModel analyze(PreparedArtifact artifact, IFrameworkAdapter adapter,
                                  ConfiguracionFronteras config);
}

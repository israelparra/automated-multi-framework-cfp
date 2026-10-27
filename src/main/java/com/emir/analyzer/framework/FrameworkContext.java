package com.emir.analyzer.framework;

import com.emir.analyzer.config.ConfiguracionFronteras;

/**
 * DTO que empaqueta el adapter y la configuración para un framework.
 * Subsistema: Framework Abstraction Layer.
 *
 * Corresponde al retorno del mensaje 8 del diagrama de secuencia.
 * resolveFrameworkContext(frameworkType) → FrameworkContext { adapter, config }
 */
public record FrameworkContext(IFrameworkAdapter adapter, ConfiguracionFronteras config) {
}

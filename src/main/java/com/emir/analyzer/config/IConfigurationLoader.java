package com.emir.analyzer.config;

/**
 * Interfaz del subsistema Configuration.
 * Contrato para carga de configuración multi-archivo con herencia.
 *
 * Corresponde al mensaje 6 del diagrama de secuencia:
 * loadForFramework(frameworkName) → ConfiguracionFronteras
 */
public interface IConfigurationLoader {

    /**
     * Carga la configuración completa para un framework específico.
     *
     * @param frameworkName Nombre del framework (spring_boot, jakarta_ee)
     * @return Configuración completa con herencia resuelta
     */
    ConfiguracionFronteras loadForFramework(String frameworkName);
}

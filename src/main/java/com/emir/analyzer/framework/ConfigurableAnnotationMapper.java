package com.emir.analyzer.framework;

import com.emir.analyzer.config.FrameworkAnnotations;

import java.util.Set;

/**
 * Implementación de IAnnotationMapper que lee la configuración desde fronteras.json.
 *
 * Reemplaza las implementaciones hardcodeadas (SpringAnnotationMapper, JakartaAnnotationMapper).
 */
public class ConfigurableAnnotationMapper implements IAnnotationMapper {

    private final FrameworkAnnotations annotations;

    public ConfigurableAnnotationMapper(FrameworkAnnotations annotations) {
        this.annotations = annotations;
    }

    @Override
    public String getRestControllerAnnotation() {
        return annotations.getRestController();
    }

    @Override
    public Set<String> getHttpMappingAnnotations() {
        return annotations.getHttpMappings();
    }

    @Override
    public Set<String> getEntityAnnotations() {
        return annotations.getEntity();
    }

    @Override
    public Set<String> getRepositoryAnnotations() {
        return annotations.getRepository();
    }

    @Override
    public String getMainApplicationAnnotation() {
        return annotations.getMainApplication();
    }

    @Override
    public String getInjectAnnotation() {
        return annotations.getInject();
    }

    @Override
    public String getTransactionalAnnotation() {
        return annotations.getTransactional();
    }
}
package com.emir.analyzer.framework.spring;

import com.emir.analyzer.framework.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * VERSIÓN 2.0: Constructor modificado para recibir mappers configurables.
 */
public class SpringBootAdapter implements IFrameworkAdapter {

    private final IAnnotationMapper annotationMapper;
    private final IComponentScanner componentScanner;

    /**
     * Constructor actualizado que recibe componentes configurables.
     */
    public SpringBootAdapter(IAnnotationMapper annotationMapper, IComponentScanner componentScanner) {
        this.annotationMapper = annotationMapper;
        this.componentScanner = componentScanner;
    }

    @Override
    public FrameworkType getFrameworkType() {
        return FrameworkType.SPRING_BOOT;
    }

    @Override
    public IAnnotationMapper getAnnotationMapper() {
        return annotationMapper;
    }

    @Override
    public IComponentScanner getComponentScanner() {
        return componentScanner;
    }

    @Override
    public boolean isRestController(Class<?> clazz) {
        return clazz.isAnnotationPresent(RestController.class) ||
                clazz.isAnnotationPresent(Controller.class);
    }

    @Override
    public boolean isEntity(Class<?> clazz) {
        for (Annotation annotation : clazz.getAnnotations()) {
            String name = annotation.annotationType().getName();
            if (name.equals("jakarta.persistence.Entity") ||
                    name.equals("javax.persistence.Entity") ||
                    name.equals("org.springframework.data.mongodb.core.mapping.Document")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDataRepository(Class<?> clazz) {
        if (!clazz.isInterface()) {
            return false;
        }
        try {
            return org.springframework.data.repository.Repository.class.isAssignableFrom(clazz);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean hasRestEndpointAnnotation(Method method) {
        return method.isAnnotationPresent(GetMapping.class) ||
                method.isAnnotationPresent(PostMapping.class) ||
                method.isAnnotationPresent(PutMapping.class) ||
                method.isAnnotationPresent(DeleteMapping.class) ||
                method.isAnnotationPresent(PatchMapping.class) ||
                method.isAnnotationPresent(RequestMapping.class);
    }

    @Override
    public String getHttpMethodType(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            if (rm.method().length > 0) {
                return rm.method()[0].name();
            }
        }
        return "UNKNOWN";
    }

    @Override
    public boolean isMainApplicationClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(SpringBootApplication.class);
    }

    @Override
    public boolean requiresMovementDeduplication() {
        return false; // Spring Boot: cada repositorio es una entidad diferente
    }
}
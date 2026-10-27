package com.emir.analyzer.framework.spring;

import com.emir.analyzer.framework.IComponentScanner;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SpringComponentScanner implements IComponentScanner {

    @Override
    public Set<String> autoDiscoverPackages(Class<?> mainAppClass) {
        Set<String> packages = new HashSet<>();
        if (mainAppClass == null) {
            return packages;
        }

        packages.addAll(extractBasePackages(mainAppClass));

        if (packages.isEmpty() && mainAppClass.getPackage() != null) {
            packages.add(mainAppClass.getPackage().getName());
        }

        return packages.stream()
                .map(p -> p.replace('.', '/'))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> extractBasePackages(Class<?> clazz) {
        Set<String> packages = new HashSet<>();

        if (clazz.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan scan = clazz.getAnnotation(ComponentScan.class);
            packages.addAll(Arrays.asList(scan.basePackages()));
        }

        if (clazz.isAnnotationPresent(EnableJpaRepositories.class)) {
            EnableJpaRepositories jpaScan = clazz.getAnnotation(EnableJpaRepositories.class);
            packages.addAll(Arrays.asList(jpaScan.basePackages()));
        }

        if (clazz.isAnnotationPresent(EntityScan.class)) {
            EntityScan entityScan = clazz.getAnnotation(EntityScan.class);
            packages.addAll(Arrays.asList(entityScan.basePackages()));
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
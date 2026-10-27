package com.emir.analyzer.engine;

import com.emir.analyzer.config.ConfiguracionFronteras;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analizador de bytecode que resuelve llamadas recursivas.
 * Subsistema: COSMIC Core Engine.
 * Movido de com.emir.analyzer. Lógica IDÉNTICA.
 */
public class BytecodeAnalyzer {

    private final URLClassLoader classLoader;
    private final Set<String> scanPackages;
    private final Set<String> repositoryInterfaceNames;
    private final ConfiguracionFronteras config;
    private final Map<String, AnalysisResult> methodCache = new HashMap<>();
    private final Set<String> allClassNames;
    private final Map<String, Set<String>> interfaceImplementationsCache = new HashMap<>();
    private boolean interfaceScanCompleted = false;

    public BytecodeAnalyzer(URLClassLoader classLoader, Set<String> scanPackages,
                            Set<String> repositoryInterfaceNames, ConfiguracionFronteras config,
                            Set<String> allClassNames) {
        this.classLoader = classLoader;
        this.scanPackages = scanPackages;
        this.repositoryInterfaceNames = repositoryInterfaceNames;
        this.config = config;
        this.allClassNames = allClassNames;
        scanAllInterfaceImplementations();
    }

    public URLClassLoader getClassLoader() { return classLoader; }

    public AnalysisResult analyzeRecursive(String owner, String methodName, String methodDescriptor,
                                           Set<String> visitedSignatures) {
        String signature = owner + "." + methodName + methodDescriptor;
        if (visitedSignatures.contains(signature)) return new AnalysisResult();
        if (methodCache.containsKey(signature)) return methodCache.get(signature);
        visitedSignatures.add(signature);
        AnalysisResult result = new AnalysisResult();
        try (InputStream is = classLoader.getResourceAsStream(owner + ".class")) {
            if (is == null) return result;
            ClassReader cr = new ClassReader(is);
            ClassVisitor cv = new RecursiveCallCounter(
                    org.objectweb.asm.Opcodes.ASM9, this, result,
                    methodName, methodDescriptor, visitedSignatures,
                    repositoryInterfaceNames, scanPackages, config);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
        } catch (Exception e) {
            System.err.println("[ERROR] Falla al analizar bytecode para: " + signature);
            e.printStackTrace();
        }
        visitedSignatures.remove(signature);
        methodCache.put(signature, result);
        return result;
    }

    private void scanAllInterfaceImplementations() {
        for (String className : allClassNames) {
            try (InputStream is = classLoader.getResourceAsStream(className + ".class")) {
                if (is == null) continue;
                ClassReader cr = new ClassReader(is);
                ClassNode cn = new ClassNode();
                cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if (cn.interfaces != null && !cn.interfaces.isEmpty()) {
                    for (Object interfaceName : cn.interfaces) {
                        String interfaceKey = interfaceName.toString();
                        interfaceImplementationsCache.computeIfAbsent(interfaceKey, k -> new HashSet<>()).add(cn.name);
                    }
                }
            } catch (Exception e) { /* Ignorar */ }
        }
        interfaceScanCompleted = true;
    }

    public Set<String> findImplementations(String interfaceName) {
        if (!interfaceScanCompleted) scanAllInterfaceImplementations();
        Set<String> implementations = interfaceImplementationsCache.get(interfaceName);
        if (implementations != null && !implementations.isEmpty()) return implementations;
        return new HashSet<>();
    }

    public Set<String> getRepositoryInterfaceNames() { return this.repositoryInterfaceNames; }
}

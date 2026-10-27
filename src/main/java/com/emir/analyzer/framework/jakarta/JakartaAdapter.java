package com.emir.analyzer.framework.jakarta;

import com.emir.analyzer.framework.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * VERSIÓN 2.0: Constructor modificado para recibir mappers configurables.
 */
public class JakartaAdapter implements IFrameworkAdapter {

    private final IAnnotationMapper annotationMapper;
    private final IComponentScanner componentScanner;

    /**
     * Constructor actualizado que recibe componentes configurables.
     */
    public JakartaAdapter(IAnnotationMapper annotationMapper, IComponentScanner componentScanner) {
        this.annotationMapper = annotationMapper;
        this.componentScanner = componentScanner;
    }

    @Override
    public FrameworkType getFrameworkType() {
        return FrameworkType.JAKARTA_EE;
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
        String className = clazz.getName();

        System.out.println("   [Jakarta Adapter] Verificando: " + className);

        // Filtrar clientes REST (no son controladores)
        if (className.endsWith("Client") ||
                className.contains(".client.") ||
                className.endsWith("ClientService")) {

            System.out.println("   ⚠️ [Jakarta] Ignorado: Es un cliente REST, no un controlador");
            return false;
        }

        // MÉTODO 1: Intentar reflexión
        try {
            for (Annotation annotation : clazz.getAnnotations()) {
                String name = annotation.annotationType().getName();
                if (name.equals("jakarta.ws.rs.Path") || name.equals("javax.ws.rs.Path")) {
                    System.out.println("   ✅ [Jakarta] Detectado recurso REST por reflexión: " + className);
                    return true;
                }
            }
        } catch (Throwable t) {
            System.out.println("   ⚠️ [Jakarta] Reflexión falló, usando bytecode para: " + className);
        }

        // MÉTODO 2: Analizar bytecode con ASM
        boolean hasPathAnnotation = hasJaxRsAnnotationInBytecode(clazz, "Path");
        if (hasPathAnnotation) {
            System.out.println("   ✅ [Jakarta] Detectado recurso REST por bytecode: " + className);
            return true;
        }

        return false;
    }

    private boolean hasJaxRsAnnotationInBytecode(Class<?> clazz, String annotationName) {
        try {
            String classInternalName = clazz.getName().replace('.', '/') + ".class";
            InputStream is = clazz.getClassLoader().getResourceAsStream(classInternalName);

            if (is == null) {
                return false;
            }

            ClassReader reader = new ClassReader(is);
            AnnotationDetectorVisitor visitor = new AnnotationDetectorVisitor(annotationName);
            reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();

            return visitor.hasAnnotation();
        } catch (Exception e) {
            System.err.println("   ❌ Error leyendo bytecode: " + e.getMessage());
            return false;
        }
    }

    private static class AnnotationDetectorVisitor extends ClassVisitor {
        private final String targetAnnotation;
        private boolean found = false;

        public AnnotationDetectorVisitor(String annotationName) {
            super(Opcodes.ASM9);
            this.targetAnnotation = annotationName;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.contains("ws/rs/" + targetAnnotation)) {
                found = true;
                System.out.println("      [ASM] Encontrada anotación: " + descriptor);
            }
            return null;
        }

        public boolean hasAnnotation() {
            return found;
        }
    }

    @Override
    public boolean isEntity(Class<?> clazz) {
        try {
            for (Annotation annotation : clazz.getAnnotations()) {
                String name = annotation.annotationType().getName();
                if (name.equals("jakarta.persistence.Entity") || name.equals("javax.persistence.Entity")) {
                    return true;
                }
            }
        } catch (Throwable t) {
            // Ignorar errores de carga
        }
        return false;
    }

    @Override
    public boolean isDataRepository(Class<?> clazz) {
        try {
            for (Annotation annotation : clazz.getAnnotations()) {
                String name = annotation.annotationType().getName();
                if (name.equals("jakarta.ejb.Stateless") ||
                        name.equals("jakarta.ejb.Stateful") ||
                        name.equals("javax.ejb.Stateless") ||
                        name.equals("javax.ejb.Stateful") ||
                        name.equals("jakarta.enterprise.context.ApplicationScoped")) {

                    try {
                        return java.util.Arrays.stream(clazz.getDeclaredFields())
                                .anyMatch(f -> f.getType().getName().contains("EntityManager"));
                    } catch (Throwable t2) {
                        return false;
                    }
                }
            }
        } catch (Throwable t) {
            // Ignorar errores
        }
        return false;
    }

    @Override
    public boolean hasRestEndpointAnnotation(Method method) {
        try {
            for (Annotation annotation : method.getAnnotations()) {
                String name = annotation.annotationType().getName();
                if (name.contains("ws.rs.GET") || name.contains("ws.rs.POST") ||
                        name.contains("ws.rs.PUT") || name.contains("ws.rs.DELETE") ||
                        name.contains("ws.rs.Path")) {
                    return true;
                }
            }
        } catch (Throwable t) {
            String methodName = method.getName().toLowerCase();
            if (methodName.startsWith("get") || methodName.startsWith("post") ||
                    methodName.startsWith("put") || methodName.startsWith("delete") ||
                    methodName.startsWith("create") || methodName.startsWith("update") ||
                    methodName.startsWith("find") || methodName.startsWith("list") ||
                    methodName.startsWith("add")) {

                System.out.println("      [Jakarta] Método detectado por heurística: " + method.getName());
                return true;
            }
        }
        return false;
    }

    @Override
    public String getHttpMethodType(Method method) {
        try {
            for (Annotation annotation : method.getAnnotations()) {
                String name = annotation.annotationType().getName();
                if (name.contains("ws.rs.GET")) return "GET";
                if (name.contains("ws.rs.POST")) return "POST";
                if (name.contains("ws.rs.PUT")) return "PUT";
                if (name.contains("ws.rs.DELETE")) return "DELETE";
            }
        } catch (Throwable t) {
            String methodName = method.getName().toLowerCase();
            if (methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("list")) return "GET";
            if (methodName.startsWith("post") || methodName.startsWith("create") || methodName.startsWith("add")) return "POST";
            if (methodName.startsWith("put") || methodName.startsWith("update")) return "PUT";
            if (methodName.startsWith("delete") || methodName.startsWith("remove")) return "DELETE";
        }
        return "UNKNOWN";
    }

    @Override
    public boolean isMainApplicationClass(Class<?> clazz) {
        try {
            for (Annotation annotation : clazz.getAnnotations()) {
                String name = annotation.annotationType().getName();
                if (name.contains("ws.rs.ApplicationPath")) {
                    return true;
                }
            }
        } catch (Throwable t) {
            // Ignorar
        }
        return false;
    }

    @Override
    public boolean requiresMovementDeduplication() {
        return false;
    }
}
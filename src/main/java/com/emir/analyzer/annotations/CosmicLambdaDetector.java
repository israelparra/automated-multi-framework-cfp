package com.emir.analyzer.annotations;

import org.objectweb.asm.*;
import java.io.InputStream;
import java.util.*;

/**
 * Detector ASM de anotaciones @CosmicLambda en bytecode.
 * Subsistema: Lambda Annotation Module. Sin cambios.
 */
public class CosmicLambdaDetector {

    public static class LambdaAnnotationInfo {
        public final List<String> movementTypes;
        public final int count;
        public final String description;

        public LambdaAnnotationInfo(List<String> movementTypes, int count, String description) {
            this.movementTypes = movementTypes;
            this.count = count;
            this.description = description;
        }
    }

    public static LambdaAnnotationInfo detectInBytecode(ClassLoader classLoader,
                                                        String className,
                                                        String targetMethodName,
                                                        String targetMethodDescriptor) {
        try {
            String classResource = className + ".class";
            InputStream is = classLoader.getResourceAsStream(classResource);
            if (is == null) return null;

            ClassReader reader = new ClassReader(is);
            CosmicLambdaVisitor visitor = new CosmicLambdaVisitor(targetMethodName, targetMethodDescriptor);
            reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            is.close();

            return visitor.getResult();
        } catch (Exception e) {
            return null;
        }
    }

    private static class CosmicLambdaVisitor extends ClassVisitor {
        private final String targetMethodName;
        private final String targetMethodDescriptor;
        private LambdaAnnotationInfo result = null;

        public CosmicLambdaVisitor(String targetMethodName, String targetMethodDescriptor) {
            super(Opcodes.ASM9);
            this.targetMethodName = targetMethodName;
            this.targetMethodDescriptor = targetMethodDescriptor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (name.equals(targetMethodName) && descriptor.equals(targetMethodDescriptor)) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (desc.contains("CosmicLambda")) {
                            return new CosmicLambdaAnnotationReader();
                        }
                        return null;
                    }
                };
            }
            return null;
        }

        private class CosmicLambdaAnnotationReader extends AnnotationVisitor {
            private List<String> movementTypes = new ArrayList<>();
            private int count = 1;
            private String description = "";

            public CosmicLambdaAnnotationReader() {
                super(Opcodes.ASM9);
            }

            @Override
            public void visit(String name, Object value) {
                if ("count".equals(name)) count = (int) value;
                if ("description".equals(name)) description = (String) value;
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                if ("value".equals(name)) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitEnum(String name, String descriptor, String value) {
                            movementTypes.add(value);
                        }
                    };
                }
                return null;
            }

            @Override
            public void visitEnd() {
                if (!movementTypes.isEmpty()) {
                    result = new LambdaAnnotationInfo(movementTypes, count, description);
                }
            }
        }

        public LambdaAnnotationInfo getResult() {
            return result;
        }
    }
}

package com.emir.analyzer.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadatos de una clase analizada.
 * Subsistema: COSMIC Core Engine.
 * Movido de com.emir.analyzer. Lógica IDÉNTICA.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClassMetaData {
    String className;
    List<Method> methods = new ArrayList<>();
    List<Field> fields = new ArrayList<>();
    List<String> annotations = new ArrayList<>();
    List<Class> dependencies = new ArrayList<>();

    public void addMethod(Method method) { this.methods.add(method); }
    public void addFields(Field field) { this.fields.add(field); }
    public void addAnnotation(String annotation) { this.annotations.add(annotation); }
    public void addDependency(Class dependency) { this.dependencies.add(dependency); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClassMetaData {\n  className: ").append(className).append(",\n");
        sb.append("  annotations: [").append(String.join(", ", annotations)).append("],\n");
        sb.append("  methods: [");
        for (Method method : methods) {
            Annotation[] anns = method.getAnnotations();
            String methodAnns = "";
            for (int i = 0; i < anns.length; i++) {
                methodAnns += anns[i].annotationType().getName() + ", ";
            }
            sb.append(method.getName()).append(": ").append(methodAnns);
        }
        if (!methods.isEmpty()) sb.setLength(sb.length() - 2);
        sb.append("],\n  fields: [");
        for (Field field : fields) {
            sb.append(field.getType().getSimpleName()).append(" ").append(field.getName()).append(", ");
        }
        if (!fields.isEmpty()) sb.setLength(sb.length() - 2);
        sb.append("],\n  dependencies: [");
        for (Class dep : dependencies) { sb.append(dep.getName()).append(", "); }
        if (!dependencies.isEmpty()) sb.setLength(sb.length() - 2);
        sb.append("]\n}");
        return sb.toString();
    }
}

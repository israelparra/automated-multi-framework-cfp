package com.emir.analyzer.framework;

/** Factory que delega al FrameworkRegistry. Sin cambios funcionales. */
public class FrameworkFactory {
    public static IFrameworkAdapter createAdapter(FrameworkType frameworkType) {
        try {
            return FrameworkRegistry.getStrategy(frameworkType).getAdapter();
        } catch (IllegalStateException e) {
            System.out.println("⚠️ Framework no detectado: " + frameworkType + ", usando Spring Boot");
            return FrameworkRegistry.getStrategy(FrameworkType.SPRING_BOOT).getAdapter();
        }
    }
    public static IFrameworkStrategy getStrategy(FrameworkType frameworkType) {
        return FrameworkRegistry.getStrategy(frameworkType);
    }
}

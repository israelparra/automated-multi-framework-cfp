package com.emir.analyzer.framework;

public enum FrameworkType {
    SPRING_BOOT("Spring Boot"),
    JAKARTA_EE("Jakarta EE"),
    UNKNOWN("Unknown");

    private final String displayName;
    FrameworkType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}

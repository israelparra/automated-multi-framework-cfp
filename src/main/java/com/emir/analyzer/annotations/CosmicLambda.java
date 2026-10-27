package com.emir.analyzer.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface CosmicLambda {
    MovementType[] value();
    int count() default 1;
    String description() default "";

    enum MovementType {
        READ, WRITE
    }
}

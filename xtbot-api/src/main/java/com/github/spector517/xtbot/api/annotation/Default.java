package com.github.spector517.xtbot.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {

    boolean boolValue() default false;
    int intValue() default 0;
    double doubleValue() default 0.0d;
    String value() default "";

    Type type() default Type.AUTODETECT;

    enum Type {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING,
        AUTODETECT
    }
}

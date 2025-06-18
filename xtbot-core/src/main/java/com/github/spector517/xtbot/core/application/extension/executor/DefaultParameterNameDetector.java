package com.github.spector517.xtbot.core.application.extension.executor;

import com.github.spector517.xtbot.api.annotation.Name;

import java.lang.reflect.Parameter;

public class DefaultParameterNameDetector implements ParameterNameDetector {

    @Override
    public String getParameterName(Parameter parameter) {
        if (parameter.isAnnotationPresent(Name.class)) {
            return parameter.getAnnotation(Name.class).value();
        }
        return parameter.getName();
    }
}

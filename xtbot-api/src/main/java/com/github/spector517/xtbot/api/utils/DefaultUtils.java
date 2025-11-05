package com.github.spector517.xtbot.api.utils;

import com.github.spector517.xtbot.api.annotation.Default;
import com.github.spector517.xtbot.api.utils.exception.PredefinedAnnotationNotFoundException;
import com.github.spector517.xtbot.api.utils.exception.UnsupportedPredefinedTypeException;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Parameter;

@UtilityClass
public class DefaultUtils {

    public Object getDefaultValue(Parameter predefinedParameter) {
        if (!predefinedParameter.isAnnotationPresent(Default.class)) {
            throw new PredefinedAnnotationNotFoundException(
                    "Parameter %s has no @Default annotation".formatted(predefinedParameter.getName())
            );
        }
        var annotation = predefinedParameter.getAnnotation(Default.class);
        return switch (annotation.type()) {
            case BOOLEAN -> annotation.boolValue();
            case INTEGER -> annotation.intValue();
            case DOUBLE -> annotation.doubleValue();
            case STRING -> annotation.value();
            case AUTODETECT -> getSmartValue(predefinedParameter, annotation);
        };
    }

    private Object getSmartValue(Parameter parameter, Default annotation) {
        if (parameter.getType() == boolean.class || parameter.getType() == Boolean.class) {
            return annotation.boolValue();
        } else if (parameter.getType() == int.class || parameter.getType() == Integer.class) {
            return annotation.intValue();
        } else if (parameter.getType() == double.class || parameter.getType() == Double.class) {
            return annotation.doubleValue();
        } else if (parameter.getType() == String.class) {
            return annotation.value();
        } else {
            throw new UnsupportedPredefinedTypeException(
                    "Unsupported predefined type %s".formatted(parameter.getType())
            );
        }
    }
}

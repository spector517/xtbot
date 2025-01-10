package com.github.spector517.xtbot.core.application.extension.executor;

import com.github.spector517.xtbot.api.annotation.Name;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ExecutorCheckerV1 implements ExecutorChecker {

    private static final Map<Class<?>, Class<?>> PRIMITIVES_MAP = Map.of(
            boolean.class, Boolean.class,
            int.class, Integer.class,
            double.class, Double.class
    );

    private static final List<Class<?>> REFERENCE_TYPES = List.of(
            String.class
    );

    private static final List<Class<?>> CONTAINER_TYPES = List.of(
            List.class,
            Map.class
    );

    private static final List<Class<?>> ALL_SUPPORTED_TYPES = Stream.of(
            PRIMITIVES_MAP.values(), REFERENCE_TYPES, CONTAINER_TYPES
    ).flatMap(Collection::stream).toList();

    @Override
    public void checkExecutor(Method method, Map<String, Object> arguments) throws ExecutorCheckFailedException {
        checkMethodModifiers(method);
        checkArgumentsNames(method, arguments);
        checkArgumentsTypes(method, arguments);
    }

    private void checkArgumentsNames(Method method, Map<String, Object> arguments)
            throws ExecutorCheckFailedException {
        var actualNames = Stream.of(method.getParameters()).map(this::getParameterName).toList();
        var expectedNames = arguments.keySet();
        var missing = actualNames.stream().filter(name -> !expectedNames.contains(name)).toList();
        var unknown = expectedNames.stream().filter(name ->!actualNames.contains(name)).toList();
        if (!missing.isEmpty()) {
            if (!unknown.isEmpty()) {
                throw new ExecutorCheckFailedException("Missing arguments: %s, but got: %s".formatted(
                    String.join(", ", missing), 
                    String.join(", ", unknown)
                ));
            } else {
                throw new ExecutorCheckFailedException(
                    "Missing arguments: ".concat(String.join(", ", missing))
                );
            }
        } else {
            if (!unknown.isEmpty()) {
                throw new ExecutorCheckFailedException("Unknown arguments: "
                        .concat(String.join(", ", unknown))
                );
            }
        }
    }

    private void checkArgumentsTypes(Method method, Map<String, Object> arguments) 
        throws ExecutorCheckFailedException 
    {
        for (var parameter : method.getParameters()) {
            var parameterName = getParameterName(parameter);
            var argument = arguments.get(parameterName);
            var expectedType = parameter.getType().isPrimitive()
                    ? PRIMITIVES_MAP.get(parameter.getType())
                    : parameter.getType();
            var actualType = argument.getClass();
            if (!ALL_SUPPORTED_TYPES.contains(expectedType)) {
                throw new ExecutorCheckFailedException("Unsupported type '%s'".formatted(expectedType));
            }
            if (CONTAINER_TYPES.contains(expectedType)) {
                if (!expectedType.isAssignableFrom(actualType)) {
                    throw new ExecutorCheckFailedException(
                            "For argument '%s' expected implementation of '%s' but got '%s'"
                                    .formatted(parameterName, parameter.getType(), actualType)
                    );
                }
            } else if (actualType != expectedType) {
                throw new ExecutorCheckFailedException(
                        "Argument '%s' has type '%s' but expected '%s"
                                .formatted(parameterName, actualType, parameter.getType())
                );
            }
        }
    }

    private void checkMethodModifiers(Method method) throws ExecutorCheckFailedException {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new ExecutorCheckFailedException(
                "Executor method '%s' must be public".formatted(method.getName())
            );
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new ExecutorCheckFailedException(
                "Executor method '%s' must be static".formatted(method.getName())
            );
        }
    }

    private String getParameterName(Parameter parameter) {
        if (parameter.isAnnotationPresent(Name.class)) {
            return parameter.getAnnotation(Name.class).value();
        }
        return parameter.getName();
    }
}

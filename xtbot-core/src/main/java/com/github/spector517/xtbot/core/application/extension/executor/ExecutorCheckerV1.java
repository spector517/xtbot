package com.github.spector517.xtbot.core.application.extension.executor;

import com.github.spector517.xtbot.api.annotation.Default;
import com.github.spector517.xtbot.api.utils.DefaultUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

    private final ParameterNameDetector parameterNameDetector;

    public ExecutorCheckerV1() {
        this.parameterNameDetector = new DefaultParameterNameDetector();
    }

    @Override
    public void checkExecutor(Method method, Map<String, Object> arguments) throws ExecutorCheckFailedException {
        checkMethodModifiers(method);
        checkArgumentsNames(method, arguments);
        checkArgumentsTypes(method, arguments);
    }

    private void checkArgumentsNames(Method method, Map<String, Object> arguments)
            throws ExecutorCheckFailedException {
        var existingNames = Stream.of(method.getParameters())
                .map(this.parameterNameDetector::getParameterName)
                .toList();
        var providedNames = arguments.keySet();
        var missingNames = Stream.of(method.getParameters())
                .filter(par -> !par.isAnnotationPresent(Default.class))
                .map(this.parameterNameDetector::getParameterName)
                .filter(name -> !providedNames.contains(name))
                .toList();
        var unknownNames = providedNames.stream()
                .filter(name ->!existingNames.contains(name))
                .toList();
        if (!missingNames.isEmpty()) {
            if (!unknownNames.isEmpty()) {
                throw new ExecutorCheckFailedException("Missing arguments: %s, but got: %s".formatted(
                    String.join(", ", missingNames),
                    String.join(", ", unknownNames)
                ));
            } else {
                throw new ExecutorCheckFailedException(
                    "Missing arguments: ".concat(String.join(", ", missingNames))
                );
            }
        }
        if (!unknownNames.isEmpty()) {
            throw new ExecutorCheckFailedException("Unknown arguments: "
                    .concat(String.join(", ", unknownNames))
            );
        }
    }

    private void checkArgumentsTypes(Method method, Map<String, Object> arguments) 
        throws ExecutorCheckFailedException 
    {
        for (var existingArgument : method.getParameters()) {
            var existingArgumentName = parameterNameDetector.getParameterName(existingArgument);
            var providedArgument = arguments.containsKey(existingArgumentName)
                    ? arguments.get(existingArgumentName)
                    : DefaultUtils.getDefaultValue(existingArgument);
            var existingType = existingArgument.getType().isPrimitive()
                    ? PRIMITIVES_MAP.get(existingArgument.getType())
                    : existingArgument.getType();
            var providedType = providedArgument.getClass();
            if (!ALL_SUPPORTED_TYPES.contains(existingType)) {
                throw new ExecutorCheckFailedException("Unsupported type '%s'".formatted(existingType));
            }
            if (CONTAINER_TYPES.contains(existingType)) {
                if (!existingType.isAssignableFrom(providedType)) {
                    throw new ExecutorCheckFailedException(
                            "For argument '%s' expected implementation of '%s' but got '%s'"
                                    .formatted(existingArgumentName, existingArgument.getType(), providedType)
                    );
                }
            } else if (providedType != existingType) {
                throw new ExecutorCheckFailedException(
                        "Argument '%s' has type '%s' but expected '%s'"
                                .formatted(existingArgumentName, providedType, existingArgument.getType())
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
}

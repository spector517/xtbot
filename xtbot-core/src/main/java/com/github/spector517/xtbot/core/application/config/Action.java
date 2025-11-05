package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.api.annotation.Default;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.api.utils.DefaultUtils;
import com.github.spector517.xtbot.core.application.extension.executor.*;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.utils.CommonUtils;
import com.github.spector517.xtbot.core.properties.data.ActionProps;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
@Accessors(fluent = true)
public class Action {

    private static final List<Class<?>> simpleTypes = List.of(
            String.class, Boolean.class, Integer.class, Long.class, Double.class
    );

    private final Gateway gateway;

    private final Method exec;
    private final Map<String, Object> templateArgs;
    @Getter
    private final String register;
    @Getter
    private final String name;
    private final ParameterNameDetector parameterNameDetector;
    private final boolean isSimpleMapping;

    Action(ActionProps props, Gateway gateway, ExecutorChecker executorChecker, ExecutorLoader executorLoader) {
        this.gateway = gateway;
        Map<String, Object> execArgs = props.args() == null ? Map.of() : props.args();
        var args = new HashMap<>(execArgs);
        this.register = Objects.requireNonNullElse(props.register(), "_");
        try {
            var executor = executorLoader.getExecutor(props.exec());
            executorChecker.checkExecutor(executor, execArgs);
            this.exec = executor;
            this.name = executor.getAnnotation(Executor.class).value();
        } catch (ExecutorNotFoundException | ExecutorCheckFailedException ex) {
            throw new LoadConfigException(ex);
        }
        var defaultArgs = Stream.of(this.exec.getParameters())
                .filter(par -> par.isAnnotationPresent(Default.class))
                .filter(par -> !execArgs.containsKey(par.getName()))
                .collect(Collectors.toMap(
                        Parameter::getName,
                        DefaultUtils::getDefaultValue
                ));
        args.putAll(defaultArgs);
        this.templateArgs = (Map<String, Object>) CommonUtils.getTemplatedMap(args, gateway.getRender());
        this.parameterNameDetector = new DefaultParameterNameDetector();
        this.isSimpleMapping = simpleTypes.contains(exec.getReturnType()) || exec.getReturnType().isPrimitive();
    }

    public Object execute(Map<String, Object> context) {
        try {
            var filledArgsMap = (Map<String, Object>) CommonUtils.getFilledMap(templateArgs, context);
            var filledArgs = getArgs(filledArgsMap);
            var result = exec.invoke(null, filledArgs);
            if (isSimpleMapping) {
                return result;
            }
            var mappingClass = exec.getReturnType().isAssignableFrom(List.class) || exec.getReturnType().isArray()
                    ? List.class
                    : Map.class;
            return gateway.getActionResultMapper().map(result, mappingClass);
        } catch (Exception ex) {
            throw new ActionExecutionException(ex);
        }
    }

    private Object[] getArgs(Map<String, Object> args) {
        return Arrays.stream(exec.getParameters())
                .map(parameter -> args.get(parameterNameDetector.getParameterName(parameter)))
                .toArray();
    }
}


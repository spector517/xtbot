package com.github.spector517.xtbot.core.application.config;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.api.annotation.Name;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.executor.*;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.application.utils.CommonUtils;
import com.github.spector517.xtbot.core.properties.ActionProps;

import lombok.Getter;
import lombok.experimental.Accessors;

@SuppressWarnings("unchecked")
@Accessors(fluent = true)
public class Action {

    private static final List<Class<?>> simpleTypes = List.of(
            String.class, Boolean.class, Integer.class, Long.class, Double.class
    );

    private final Mapper<Map<String, Object>, UpdateData> contextMapper;
    private final Gateway gateway;

    private final Method exec;
    private final Map<String, Object> templateArgs;
    @Getter
    private final String register;
    @Getter
    private final String name;
    private final ParameterNameDetector parameterNameDetector;

    Action(ActionProps props, Gateway gateway, ExecutorChecker executorChecker, ExecutorLoader executorLoader) {
        this.contextMapper = gateway.getContextMapper();
        this.gateway = gateway;
        this.templateArgs = (Map<String, Object>) CommonUtils.getTemplatedMap(props.args(), gateway.getRender());
        this.register = Objects.requireNonNullElse(props.register(), "");
        try {
            var executor = executorLoader.getExecutor(props.exec());
            executorChecker.checkExecutor(executor, props.args());
            this.exec = executor;
            this.name = executor.getAnnotation(Executor.class).value();
        } catch (ExecutorNotFoundException | ExecutorCheckFailedException ex) {
            throw new LoadConfigException(ex);
        }
        this.parameterNameDetector = new DefaultParameterNameDetector();
    }

    public Object execute(UpdateData updateData) {
        var isSimpleMapping = simpleTypes.contains(exec.getReturnType()) || exec.getReturnType().isPrimitive();
        try {
            var filledArgsMap = (Map<String, Object>) CommonUtils.getFilledMap(
                templateArgs, contextMapper.map(updateData)
            );
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


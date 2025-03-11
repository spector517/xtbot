package com.github.spector517.xtbot.core.application.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorCheckFailedException;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorNotFoundException;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
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

    private final ComponentsContainer container;
    private final Mapper<UpdateData, Map<String, Object>> contextMapper;

    private final Method exec;
    private final Map<String, Object> templateArgs;
    @Getter
    private final String register;
    @Getter
    private final String name;

    Action(ActionProps props, ComponentsContainer container) {
        this.container = container;
        this.contextMapper = container.updateDataToContextMapper();
        this.templateArgs = (Map<String, Object>) CommonUtils.getTemplatedMap(props.args(), container.render());
        this.register = Objects.requireNonNullElse(props.register(), "");
        try {
            var executor = container.executorLoader().getExecutor(props.exec());
            container.executorChecker().checkExecutor(executor, props.args());
            this.exec = executor;
            this.name = executor.getAnnotation(Executor.class).value();
        } catch (ExecutorNotFoundException | ExecutorCheckFailedException ex) {
            throw new LoadConfigException(ex);
        }
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
            var mappingClass = exec.getReturnType().isAssignableFrom(List.class) ? List.class : Map.class;
            return container.jsonObjectMapper().convertValue(result, mappingClass);
        } catch (Exception ex) {
            throw new ActionExecutionException(ex);
        }
    }

    private Object[] getArgs(Map<String, Object> args) {
        return Arrays.stream(exec.getParameters())
                .map(parameter -> args.get(parameter.getName()))
                .toArray();
    }
}

package com.github.spector517.xtbot.core.application.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.spector517.xtbot.core.application.config.Template;
import com.github.spector517.xtbot.core.application.render.Render;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonUtils {

    public Map<?, ?> getTemplatedMap(Map<?, ?> argsMap, Render render) {
        return argsMap.entrySet().stream().collect(
                Collectors.toMap(
                        entry -> getTemplatedObject(entry.getKey(), render),
                        entry -> getTemplatedObject(entry.getValue(), render)
                )
        );
    }

    public Map<?, ?> getFilledMap(Map<?, ?> map, Map<String, Object> context) {
        return map.entrySet().stream().collect(
                Collectors.toMap(
                        entry -> getFilledObject(entry.getKey(), context),
                        entry -> getFilledObject(entry.getValue(), context)
                )
        );
    }

    private List<?> getTemplatedList(List<?> argsList, Render render) {
        return argsList.stream().map(item -> getTemplatedObject(item, render)).toList();
    }

    private Object getTemplatedObject(Object object, Render render) {
        if (object instanceof Map<?, ?> map) {
            return getTemplatedMap(map, render);
        } else if (object instanceof List<?> list) {
            return getTemplatedList(list, render);
        } else if (object instanceof String string) {
            return new Template(render, string);
        }
        return object;
    }

    private List<?> getFilledList(List<?> list, Map<String, Object> context) {
        return list.stream()
                .map(item -> getFilledObject(item, context))
                .toList();
    }

    private Object getFilledObject(Object object, Map<String, Object> context) {
        if (object instanceof Map<?, ?> map) {
            return getFilledMap(map, context);
        } else if (object instanceof List<?> list) {
            return getFilledList(list, context);
        } else if (object instanceof Template template) {
            return template.value(context);
        }
        return object;
    }
}

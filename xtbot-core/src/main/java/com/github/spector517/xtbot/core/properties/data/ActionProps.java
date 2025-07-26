package com.github.spector517.xtbot.core.properties.data;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class ActionProps {

    private static final String REGISTER_PROPERTY = "register";

    private String exec;
    private Map<String, Object> args;
    private String register;

    @JsonAnySetter
    @SuppressWarnings("unchecked")
    public void init(String key, Object value) {
        if (key.equals(REGISTER_PROPERTY) && value instanceof String strValue) {
            register = strValue;
            return;
        }
        if (exec != null) {
            throw new IllegalArgumentException(
                    "Executor '%s' already defined, cannot set one more '%s'".formatted(exec, key)
            );
        }
        if (value instanceof Map<?, ?> || value == null) {
            exec = key;
            args = value != null ? (Map<String, Object>) value : null;
            return;
        }
        throw new IllegalArgumentException("Unknown action property '%s' with value '%s'".formatted(key, value));
    }
}

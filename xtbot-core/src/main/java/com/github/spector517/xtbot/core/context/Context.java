package com.github.spector517.xtbot.core.context;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

    private static final List<String> RESERVED_VARS_NAMES = List.of("update", "client");

    @Getter
    private final UpdateContext update;
    @Getter
    private final ClientContext client;
    @JsonAnyGetter
    private final Map<String, Object> vars;

    public Context(UpdateData updateData) {
        this.update = new UpdateContext(updateData);
        this.client = new ClientContext(updateData.client());
        this.vars = getVarsWithoutReserved(updateData.client().stageVars());
    }

    private Map<String, Object> getVarsWithoutReserved(Map<String, Object> vars) {
        var normalizedVars = new HashMap<String, Object>();
        for (var entry : vars.entrySet()) {
            if (!RESERVED_VARS_NAMES.contains(entry.getKey())) {
                normalizedVars.put(entry.getKey(), entry.getValue());
            } else {
                log.warn("Variable '{}' is reserved and will not be included in context", entry.getKey());
            }
        }
        return normalizedVars;
    }
}

package com.github.spector517.xtbot.core.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;

import lombok.Getter;

import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

    private final UpdateContext update;
    private final ClientContext client;
    private final Map<String, Object> vars;

    public Context(UpdateData updateData) {
        this.update = new UpdateContext(updateData);
        this.client = new ClientContext(updateData.client());
        this.vars = updateData.client().stageVars();
    }
}

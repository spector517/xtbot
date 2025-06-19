package com.github.spector517.xtbot.core.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientContext {

    @JsonProperty("id")
    private final long externalId;
    private final String name;
    private final String stage;
    @JsonProperty("sent_message_ids")
    private final List<Integer> sentMessageIds;
    @JsonProperty("previous_stages")
    private final List<String> previousStages;
    private final Map<String, Object> vars;

    public ClientContext(ClientData clientData) {
        this.externalId = clientData.externalId();
        this.name = clientData.name();
        this.stage = clientData.stageName();
        this.sentMessageIds = clientData.sentMessageIds();
        this.previousStages = clientData.previousStages();
        this.vars = clientData.additionalVars();
    }
}

package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ClientDataToEntityMapper implements Mapper<ClientEntity, ClientData> {

    private static final String DEFAULT_CONTEXT_OBJECT = "{}";
    private static final String DEFAULT_CONTEXT_LIST = "[]";

    private final ObjectMapper objectMapper;

    @Override
    public ClientEntity map(ClientData clientData, Object... ignored) {
        var stages = new ArrayList<>(clientData.previousStages());
        if (clientData.stageName() != null) {
            stages.add(clientData.stageName());
        }
        return new ClientEntity()
                .externalId(clientData.externalId())
                .name(clientData.name())
                .sentMessageIds(clientData.sentMessageIds())
                .stages(stages)
                .stageInitiated(clientData.stageInitiated())
                .stageCompleted(clientData.stageCompleted())
                .additionalVars(mapToJson(clientData.additionalVars()))
                .stageVars(mapToJson(clientData.stageVars()));
    }

    private String mapToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            return object instanceof List ? DEFAULT_CONTEXT_LIST : DEFAULT_CONTEXT_OBJECT;
        }
    }
}

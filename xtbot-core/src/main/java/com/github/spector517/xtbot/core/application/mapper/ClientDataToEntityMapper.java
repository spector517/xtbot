package com.github.spector517.xtbot.core.application.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.repository.ClientEntity;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ClientDataToEntityMapper implements Mapper<ClientData, ClientEntity> {

    private static final String DEFAULT_CONTEXT_OBJECT = "{}";
    private static final String DEFAULT_CONTEXT_LIST = "[]";

    private final ObjectMapper objectMapper;

    @Override
    public ClientEntity map(ClientData clientData) throws MappingException {
        return new ClientEntity()
                .id(clientData.id())
                .externalId(clientData.externalId())
                .name(clientData.name())
                .currentStage(clientData.currentStage())
                .currentStageInitiated(clientData.currentStageInitiated())
                .currentStageCompleted(clientData.currentStageCompleted())
                .previousSendedMessageId(clientData.previousSendedMessageId())
                .previousStages(mapToJson(clientData.previousStages()))
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

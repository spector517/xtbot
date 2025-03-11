package com.github.spector517.xtbot.core.application.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.repository.ClientEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class ClientEntityToDataMapper implements Mapper<ClientEntity, ClientData> {

    private final ObjectMapper objectMapper;

    @Override
    public ClientData map(ClientEntity clientEntity) throws MappingException {
        var clientData = new ClientData()
                .externalId(clientEntity.externalId())
                .name(clientEntity.name())
                .currentStage(clientEntity.currentStage())
                .currentStageInitiated(clientEntity.currentStageInitiated())
                .currentStageCompleted(clientEntity.currentStageCompleted())
                .previousSendedMessageId(clientEntity.previousSendedMessageId());

        var previousStages = mapFromJson(clientEntity.previousStages(), new TypeReference<List<String>>(){});
        clientData.previousStages(previousStages != null ? previousStages : List.of());

        var additionalVars = mapFromJson(
            clientEntity.additionalVars(),
            new TypeReference<Map<String, Object>>(){}
        );
        clientData.additionalVars(additionalVars != null ? additionalVars : Map.of());

        var stageVars = mapFromJson(clientEntity.stageVars(), new TypeReference<Map<String, Object>>(){});
        clientData.stageVars(stageVars != null ? stageVars : Map.of());
        return clientData;
    }

    private <T> T mapFromJson(String json, TypeReference<T> typeRef) throws MappingException {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException ex) {
            log.debug("Error mapping '{}' to {}", json, typeRef.getType());
            log.error("Error mapping client entity to data", ex);
            throw new MappingException(ex);
        }
    }
}

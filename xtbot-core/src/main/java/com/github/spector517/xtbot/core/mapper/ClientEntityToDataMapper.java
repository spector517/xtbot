package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ClientEntityToDataMapper implements Mapper<ClientData, ClientEntity> {

    private final ObjectMapper objectMapper;

    @Override
    public ClientData map(ClientEntity clientEntity, Object... ignored) throws MappingException {
        var clientData = new ClientData()
                .externalId(clientEntity.externalId())
                .name(clientEntity.name());
        clientData.sentMessageIds(clientEntity.sentMessageIds());
        if (clientEntity.stages().size() > 1) {
            clientData.previousStages(clientEntity.stages().subList(0, clientEntity.stages().size() - 1));
        }

        if (!clientEntity.stages().isEmpty()) {
            clientData.bindNewStage(clientEntity.stages().getLast());
        }
        if (clientEntity.stageInitiated()) {
            clientData.setStageInitiated();
        }
        if (clientEntity.stageCompleted()) {
            clientData.setStageCompleted();
        }

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
            throw new MappingException(ex);
        }
    }
}

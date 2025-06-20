package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;

import java.util.ArrayList;
import java.util.List;

public class ClientDataToEntityMapper implements Mapper<ClientEntity, ClientData> {

    public static final int DEFAULT_ENTITY_COLLECTION_LIMIT = 10;

    private static final String DEFAULT_CONTEXT_OBJECT = "{}";
    private static final String DEFAULT_CONTEXT_LIST = "[]";

    private final ObjectMapper objectMapper;
    private final int collectionLimit;

    public ClientDataToEntityMapper(ObjectMapper  objectMapper) {
        this(objectMapper, DEFAULT_ENTITY_COLLECTION_LIMIT);
    }

    public ClientDataToEntityMapper(ObjectMapper objectMapper, int collectionLimit) {
        this.objectMapper = objectMapper;
        this.collectionLimit = collectionLimit > 0 ? collectionLimit : DEFAULT_ENTITY_COLLECTION_LIMIT;
    }

    @Override
    public ClientEntity map(ClientData clientData, Object... ignored) {
        var stages = new ArrayList<>(clientData.previousStages());
        if (clientData.stageName() != null) {
            stages.add(clientData.stageName());
        }
        return new ClientEntity()
                .externalId(clientData.externalId())
                .name(clientData.name())
                .sentMessageIds(shorList(clientData.sentMessageIds()))
                .stages(shorList(stages))
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

    private <T> List<T> shorList(List<T> list) {
        var skip = list.size() - collectionLimit;
        if (skip <= 0) {
            return list;
        }
        return list.stream()
                .skip(list.size() - collectionLimit)
                .toList();
    }
}

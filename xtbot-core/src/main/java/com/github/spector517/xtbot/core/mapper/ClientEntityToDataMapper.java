package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ChatMessage;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.MessageType;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.core.repository.entity.MessageEntity;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ClientEntityToDataMapper implements Mapper<ClientData, ClientEntity> {

    public static final int MESSAGES_HISTORY_LIMIT = 10;

    private final ObjectMapper objectMapper;

    @Override
    public ClientData map(ClientEntity clientEntity, Object... ignored) throws MappingException {
        var clientData = new ClientData()
                .externalId(clientEntity.externalId())
                .name(clientEntity.name());

        // Load the last N messages from entity into ClientData
        var allMessages = clientEntity.messages() != null ? clientEntity.messages() : List.<MessageEntity>of();
        var latestMessages = allMessages.stream()
                .sorted(Comparator.comparingLong(m -> m.id() != null ? m.id() : 0L))
                .skip(Math.max(0, allMessages.size() - MESSAGES_HISTORY_LIMIT))
                .map(m -> new ChatMessage(m.telegramMessageId(), m.text(), m.sentAt(), toDataMessageType(m.type())))
                .toList();
        clientData.messages(latestMessages);

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

    private static MessageType toDataMessageType(com.github.spector517.xtbot.core.repository.entity.MessageType entityType) {
        if (entityType == null) return null;
        return switch (entityType) {
            case BOT -> MessageType.BOT;
            case USER -> MessageType.USER;
        };
    }

    private <T> T mapFromJson(String json, TypeReference<T> typeRef) throws MappingException {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException ex) {
            throw new MappingException(ex);
        }
    }
}

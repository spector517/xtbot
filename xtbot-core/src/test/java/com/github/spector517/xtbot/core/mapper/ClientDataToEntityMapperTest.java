package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ChatMessage;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.MessageType;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.core.repository.entity.MessageEntity;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientDataToEntityMapperTest {

    private ObjectMapper objectMapper;
    private String userName;
    private long externalId;
    private String currentStage;
    private List<String> previousStages;
    private Map<String, Object> additionalVars;
    private Map<String, Object> stageVars;
    private LocalDateTime fixedTime;

    private ClientDataToEntityMapper clientDataToEntityMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        userName = "User";
        externalId = 11;
        currentStage = "TestStage";
        previousStages = List.of("Stage1", "Stage2");
        additionalVars = Map.of("key1", "value1");
        stageVars = Map.of("key2", "value2");
        fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        clientDataToEntityMapper = new ClientDataToEntityMapper(objectMapper);
    }

    @Test
    @DisplayName("All fields")
    @SneakyThrows
    void map_0() {
        var chatMessages = List.of(
                new ChatMessage(10, "bot message", fixedTime, MessageType.BOT),
                new ChatMessage(11, "user message", fixedTime, MessageType.USER)
        );
        var clientData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .messages(chatMessages)
                .previousStages(previousStages)
                .bindNewStage(currentStage)
                .setStageInitiated()
                .additionalVars(additionalVars)
                .stageVars(stageVars);
        var expectedStages = new ArrayList<>(previousStages);
        expectedStages.add(currentStage);
        var expectedMessages = List.of(
                new MessageEntity().telegramMessageId(10).text("bot message").sentAt(fixedTime)
                        .type(com.github.spector517.xtbot.core.repository.entity.MessageType.BOT),
                new MessageEntity().telegramMessageId(11).text("user message").sentAt(fixedTime)
                        .type(com.github.spector517.xtbot.core.repository.entity.MessageType.USER)
        );
        var expectedEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .stageInitiated(true)
                .stageCompleted(false)
                .messages(expectedMessages)
                .stages(expectedStages)
                .additionalVars(objectMapper.writeValueAsString(additionalVars))
                .stageVars(objectMapper.writeValueAsString(stageVars));

        var actualEntity = clientDataToEntityMapper.map(clientData);

        assertEquals(expectedEntity, actualEntity);
    }

    @Test
    @DisplayName("No previousStages and no messages")
    @SneakyThrows
    void map_1() {
        var clientData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .previousStages(List.of())
                .bindNewStage(currentStage)
                .setStageInitiated()
                .additionalVars(Map.of())
                .stageVars(stageVars);
        var expectedEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .stages(List.of(currentStage))
                .stageInitiated(true)
                .stageCompleted(false)
                .messages(List.of())
                .additionalVars("{}")
                .stageVars(objectMapper.writeValueAsString(stageVars));

        var actualEntity = clientDataToEntityMapper.map(clientData);

        assertEquals(expectedEntity, actualEntity);
    }

    @Test
    @DisplayName("Limits collection of stages")
    @SneakyThrows
    void map_2() {
        var stages = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        var chatMessages = List.of(
                new ChatMessage(1, "msg1", fixedTime, MessageType.BOT),
                new ChatMessage(2, "msg2", fixedTime, MessageType.USER)
        );
        var clientData = new ClientData()
                .stageVars(Map.of())
                .additionalVars(Map.of())
                .messages(chatMessages);
        stages.forEach(clientData::bindNewStage);
        var expectedMessages = List.of(
                new MessageEntity().telegramMessageId(1).text("msg1").sentAt(fixedTime)
                        .type(com.github.spector517.xtbot.core.repository.entity.MessageType.BOT),
                new MessageEntity().telegramMessageId(2).text("msg2").sentAt(fixedTime)
                        .type(com.github.spector517.xtbot.core.repository.entity.MessageType.USER)
        );
        var expectedEntity = new ClientEntity()
                .externalId(0L)
                .stageInitiated(false)
                .stageCompleted(false)
                .stages(stages.subList(2, stages.size()))
                .messages(expectedMessages)
                .additionalVars("{}")
                .stageVars("{}");

        var actualEntity = clientDataToEntityMapper.map(clientData);

        assertEquals(expectedEntity, actualEntity);
    }
}
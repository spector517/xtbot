package com.github.spector517.xtbot.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ChatMessage;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.MessageType;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.core.repository.entity.MessageEntity;

import lombok.SneakyThrows;

class ClientEntityToDataMapperTest {

    private ObjectMapper objectMapper;
    private String userName;
    private long externalId;
    private String currentStage;
    private List<String> previousStages;
    private Map<String, Object> additionalVars;
    private LocalDateTime fixedTime;
    private ClientEntityToDataMapper clientEntityToDataMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        userName = "User";
        externalId = 11;
        currentStage = "TestStage";
        previousStages = List.of("Stage1", "Stage2");
        additionalVars = Map.of("key1", "value1");
        fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        clientEntityToDataMapper = new ClientEntityToDataMapper(objectMapper);
    }

    @Test
    @DisplayName("All fields with messages")
    @SneakyThrows
    void map_0() {
        var stages = new ArrayList<>(previousStages);
        stages.add(currentStage);
        var botMessage = new MessageEntity()
                .id(1L)
                .telegramMessageId(100)
                .text("hello from bot")
                .sentAt(fixedTime)
                .type(com.github.spector517.xtbot.core.repository.entity.MessageType.BOT);
        var userMessage = new MessageEntity()
                .id(2L)
                .telegramMessageId(101)
                .text("hello from user")
                .sentAt(fixedTime)
                .type(com.github.spector517.xtbot.core.repository.entity.MessageType.USER);
        var clientEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .stages(stages)
                .stageInitiated(true)
                .stageCompleted(false)
                .messages(List.of(botMessage, userMessage))
                .additionalVars(objectMapper.writeValueAsString(additionalVars))
                .stageVars("{\"key2\":\"value2\"}");
        var expectedMessages = List.of(
                new ChatMessage(100, "hello from bot", fixedTime, MessageType.BOT),
                new ChatMessage(101, "hello from user", fixedTime, MessageType.USER)
        );
        var expectedData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .messages(expectedMessages)
                .previousStages(previousStages)
                .bindNewStage(currentStage)
                .setStageInitiated()
                .additionalVars(additionalVars)
                .stageVars(Map.of("key2", "value2"));

        var actualData = clientEntityToDataMapper.map(clientEntity);

        assertEquals(expectedData, actualData);
    }

    @Test
    @DisplayName("No previousStages, no messages")
    @SneakyThrows
    void map_1() {
        var clientEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .stages(List.of(currentStage))
                .stageInitiated(true)
                .stageCompleted(false)
                .messages(List.of())
                .additionalVars("{}")
                .stageVars("{}");
        var expectedData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .messages(List.of())
                .previousStages(List.of())
                .bindNewStage(currentStage)
                .setStageInitiated()
                .previousStages(List.of())
                .additionalVars(Map.of())
                .stageVars(Map.of());

        var actualData = clientEntityToDataMapper.map(clientEntity);

        assertEquals(expectedData, actualData);
    }

    @Test
    @DisplayName("Loads only last 10 messages")
    @SneakyThrows
    void map_2() {
        // Create 12 messages — expect only last 10 to be loaded
        var messageEntities = new ArrayList<MessageEntity>();
        for (int i = 1; i <= 12; i++) {
            messageEntities.add(new MessageEntity()
                    .id((long) i)
                    .telegramMessageId(i)
                    .text("msg " + i)
                    .sentAt(fixedTime)
                    .type(i % 2 == 0
                            ? com.github.spector517.xtbot.core.repository.entity.MessageType.USER
                            : com.github.spector517.xtbot.core.repository.entity.MessageType.BOT));
        }
        var clientEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .stages(List.of(currentStage))
                .stageInitiated(false)
                .stageCompleted(false)
                .messages(messageEntities)
                .additionalVars("{}")
                .stageVars("{}");

        var actualData = clientEntityToDataMapper.map(clientEntity);

        assertEquals(ClientEntityToDataMapper.MESSAGES_HISTORY_LIMIT, actualData.messages().size());
        // Last 10 messages are ids 3..12
        assertEquals(3, actualData.messages().getFirst().telegramMessageId());
        assertEquals(12, actualData.messages().getLast().telegramMessageId());
        // sentMessageIds derived from BOT messages in last 10 (ids 3,5,7,9,11 — odd)
        assertEquals(List.of(3, 5, 7, 9, 11), actualData.sentMessageIds());
    }
}
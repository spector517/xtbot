package com.github.spector517.xtbot.core.application.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.repository.ClientEntity;

import lombok.SneakyThrows;

class ClientEntityToDataMapperTest {

    private ObjectMapper objectMapper;
    private String userName;
    private long externalId;
    private String currentStage;
    private int previousSendedMessageId;
    private List<String> previousStages;
    private Map<String, Object> additionalVars;
    private ClientEntityToDataMapper clientEntityToDataMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        userName = "User";
        externalId = 11;
        currentStage = "TestStage";
        previousSendedMessageId = 123;
        previousStages = List.of("Stage1", "Stage2");
        additionalVars = Map.of("key1", "value1");
        Map.of("key2", "value2");
        clientEntityToDataMapper = new ClientEntityToDataMapper(objectMapper);
    }

    @Test
    @DisplayName("All fields")
    @SneakyThrows
    void map_0() {
        var clientEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .currentStage(currentStage)
                .currentStageInitiated(true)
                .currentStageCompleted(false)
                .previousSendedMessageId(previousSendedMessageId)
                .previousStages(objectMapper.writeValueAsString(previousStages))
                .additionalVars(objectMapper.writeValueAsString(additionalVars))
                .stageVars("{\"key2\":\"value2\"}");
        var expectedData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .currentStage(currentStage)
                .currentStageInitiated(true)
                .currentStageCompleted(false)
                .previousSendedMessageId(previousSendedMessageId)
                .previousStages(previousStages)
                .additionalVars(additionalVars)
                .stageVars(Map.of("key2", "value2"));

        var actualData = clientEntityToDataMapper.map(clientEntity);

        assertEquals(expectedData, actualData);
    }

    @Test
    @DisplayName("No previousStages and additionalVars")
    @SneakyThrows
    void map_1() {
        var clientEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .currentStage(currentStage)
                .currentStageInitiated(true)
                .currentStageCompleted(false)
                .previousStages("[]")
                .additionalVars("{}")
                .stageVars("{}");
        var expectedData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .currentStage(currentStage)
                .currentStageInitiated(true)
                .currentStageCompleted(false)
                .previousStages(List.of())
                .additionalVars(Map.of())
                .stageVars(Map.of());

        var actualData = clientEntityToDataMapper.map(clientEntity);

        assertEquals(expectedData, actualData);
    }
}
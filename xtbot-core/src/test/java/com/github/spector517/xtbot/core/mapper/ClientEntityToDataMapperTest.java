package com.github.spector517.xtbot.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;

import lombok.SneakyThrows;

class ClientEntityToDataMapperTest {

    private ObjectMapper objectMapper;
    private String userName;
    private long externalId;
    private String currentStage;
    private int previousSentMessageId;
    private List<String> previousStages;
    private Map<String, Object> additionalVars;
    private ClientEntityToDataMapper clientEntityToDataMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        userName = "User";
        externalId = 11;
        currentStage = "TestStage";
        previousSentMessageId = 123;
        previousStages = List.of("Stage1", "Stage2");
        additionalVars = Map.of("key1", "value1");
        clientEntityToDataMapper = new ClientEntityToDataMapper(objectMapper);
    }

    @Test
    @DisplayName("All fields")
    @SneakyThrows
    void map_0() {
        var stages = new ArrayList<>(previousStages);
        stages.add(currentStage);
        var clientEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .stages(List.of(currentStage))
                .stageInitiated(true)
                .stageCompleted(false)
                .sentMessageIds(List.of(previousSentMessageId))
                .stages(stages)
                .additionalVars(objectMapper.writeValueAsString(additionalVars))
                .stageVars("{\"key2\":\"value2\"}");
        var expectedData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .sentMessageIds(List.of(previousSentMessageId))
                .previousStages(previousStages)
                .bindNewStage(currentStage)
                .setStageInitiated()
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
                .stages(List.of(currentStage))
                .stageInitiated(true)
                .stageCompleted(false)
                .additionalVars("{}")
                .stageVars("{}");
        var expectedData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .previousStages(List.of())
                .bindNewStage(currentStage)
                .setStageInitiated()
                .previousStages(List.of())
                .additionalVars(Map.of())
                .stageVars(Map.of());

        var actualData = clientEntityToDataMapper.map(clientEntity);

        assertEquals(expectedData, actualData);
    }
}
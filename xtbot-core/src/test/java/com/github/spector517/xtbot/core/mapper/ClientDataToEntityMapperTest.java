package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    private List<Integer> previousSentMessageIds;
    private Map<String, Object> additionalVars;
    private Map<String, Object> stageVars;

    private ClientDataToEntityMapper clientDataToEntityMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        userName = "User";
        externalId = 11;
        currentStage = "TestStage";
        previousStages = List.of("Stage1", "Stage2");
        previousSentMessageIds = List.of(1, 2, 3);
        additionalVars = Map.of("key1", "value1");
        stageVars = Map.of("key2", "value2");
        clientDataToEntityMapper = new ClientDataToEntityMapper(objectMapper);
    }

    @Test
    @DisplayName("All fields")
    @SneakyThrows
    void map_0() {
        var clientData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .sentMessageIds(previousSentMessageIds)
                .previousStages(previousStages)
                .bindNewStage(currentStage)
                .setStageInitiated()
                .additionalVars(additionalVars)
                .stageVars(stageVars);
        var expectedStages = new ArrayList<>(previousStages);
        expectedStages.add(currentStage);
        var expectedEntity = new ClientEntity()
                .externalId(externalId)
                .name(userName)
                .stageInitiated(true)
                .stageCompleted(false)
                .sentMessageIds(previousSentMessageIds)
                .stages(expectedStages)
                .additionalVars(objectMapper.writeValueAsString(additionalVars))
                .stageVars(objectMapper.writeValueAsString(stageVars));

        var actualEntity = clientDataToEntityMapper.map(clientData);

        assertEquals(expectedEntity, actualEntity);
    }

    @Test
    @DisplayName("No previousStages and additionalVars")
    @SneakyThrows
    void map_1() {
        var clientData = new ClientData()
                .externalId(externalId)
                .name(userName)
                .sentMessageIds(previousSentMessageIds)
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
                .sentMessageIds(previousSentMessageIds)
                .stages(List.of(currentStage))
                .additionalVars("{}")
                .stageVars(objectMapper.writeValueAsString(stageVars));

        var actualEntity = clientDataToEntityMapper.map(clientData);

        assertEquals(expectedEntity, actualEntity);
    }

    @Test
    @DisplayName("Limits collection of stages and sentMessageIds")
    void map_2() {
        var stages = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        var sentMessageIds = List.of(1, 2, 3, 4, 5, 6);
        var clientData = new ClientData()
                .stageVars(Map.of())
                .additionalVars(Map.of());
        stages.forEach(clientData::bindNewStage);
        sentMessageIds.forEach(clientData::registerSentMessageId);
        var expectedEntity = new ClientEntity()
                .externalId(0L)
                .stageInitiated(false)
                .stageCompleted(false)
                .stages(stages.subList(2, stages.size()))
                .sentMessageIds(sentMessageIds)
                .additionalVars("{}")
                .stageVars("{}");

        var actualEntity = clientDataToEntityMapper.map(clientData);

        assertEquals(expectedEntity, actualEntity);
    }
}
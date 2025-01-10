package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.*;
import com.github.spector517.xtbot.core.application.mapper.MappingException;
import com.github.spector517.xtbot.core.context.Context;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class UpdateDataToContextMapperTest {

    private Map<String, Object> expectedContextMap;
    private UpdateData updateData;

    @BeforeEach
    void setUp() {
        var id = 1;
        var externalId = 11L;
        var name = "cl1";
        var currentStage = "stage3";
        var previousSendedMessageId = 123;
        var previousStages = List.of("stage1", "stage2");
        var stageVars = Map.of("key1", (Object)"value1", "key2", (Object) "value2");
        var additionalVars = Map.of("key3", (Object) "value3", "key4", (Object) "value4");
        var messageId = 1111;
        var messageText = "test";
        var clientData = new ClientData()
                .id(id)
                .externalId(externalId)
                .name(name)
                .currentStage(currentStage)
                .currentStageInitiated(true)
                .currentStageCompleted(false)
                .previousSendedMessageId(previousSendedMessageId)
                .previousStages(previousStages)
                .stageVars(stageVars)
                .additionalVars(additionalVars);
        var messageData = new MessageData()
                .id(messageId)
                .text("test");
        updateData = new UpdateData()
                .type(Type.MESSAGE)
                .chatId(1)
                .message(messageData)
                .client(clientData);
        expectedContextMap = Map.of(
                "update", Map.of(
                        "message", Map.of(
                                "id", messageId,
                                "text", messageText
                        )
                ),
                "client", Map.of(
                        "id", externalId,
                        "name", name,
                        "stage", currentStage,
                        "previous_sended_message_id", previousSendedMessageId,
                        "previous_stages", previousStages,
                        "vars", additionalVars
                ),
                "vars", stageVars
        );
    }

    @Test
    @DisplayName("To Context Map: message update")
    @SneakyThrows
    void contextMap_0() {
        var mapper = new ObjectMapper();
        var actualContextMap = new UpdateDataToContextMapper(mapper).map(updateData);
        assertEquals(expectedContextMap, actualContextMap);
    }

    @Test
    @DisplayName("To Context String: callback update")
    @SneakyThrows
    void contextMap_1() {
        var mapper = new ObjectMapper();
        var data = "test";
        updateData.message(null);
        updateData.callback(new CallbackData()
                .data(data)
        );
        var contextMap = new HashMap<>(expectedContextMap);
        contextMap.put(
                "update", Map.of(
                        "callback", Map.of(
                                "data", data
                        )
                )
        );

        var actualContextString = new UpdateDataToContextMapper(mapper).map(updateData);
        assertEquals(contextMap, actualContextString);
    }

    @Test
    @DisplayName("To Context Map: mapping error")
    void contextMap_2() {
        var mapper = Mockito.mock(ObjectMapper.class);
        when(mapper.convertValue(any(Context.class), any(TypeReference.class)))
                .thenThrow(new IllegalStateException("test"));

        assertThrows(
                MappingException.class,
                () -> new UpdateDataToContextMapper(mapper).map(updateData)
        );
    }
}
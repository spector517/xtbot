package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.*;
import com.github.spector517.xtbot.core.repository.ClientNotFoundException;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TgSdkUpdateToDataMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String userName = "User";
    private final long chatId = 111;
    private final long externalId = 11;
    private final int messageId = 1111;
    private final String initialStageName = "initial";
    private final String messageText = "text";
    private final String callbackData = "data";
    private final Mapper<ClientData, ClientEntity> clientEntityToDataMapper =
        new ClientEntityToDataMapper(objectMapper);

    private final ClientRepository clientRepository = Mockito.mock(ClientRepository.class);


    private UpdateData prefilledUpdateData;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        long id = 1;
        var previousStage = "Stage0";
        var currentStage = "Stage1";
        var additionalVars = Map.of("var1", (Object) "value1");
        var stageVars = Map.of("var2", (Object) "value2");
        var clientEntity = new ClientEntity()
                .id(id)
                .externalId(externalId)
                .name(userName)
                .stages(List.of(previousStage, currentStage))
                .stageInitiated(true)
                .stageCompleted(false)
                .additionalVars(objectMapper.writeValueAsString(additionalVars))
                .stageVars(objectMapper.writeValueAsString(stageVars));
        when(clientRepository.findByExternalId(externalId)).thenReturn(clientEntity);
        prefilledUpdateData = new UpdateData()
                .client(new ClientData()
                        .externalId(externalId)
                        .name(userName)
                        .previousStages(List.of(previousStage))
                        .bindNewStage(currentStage)
                        .setStageInitiated()
                        .additionalVars(additionalVars)
                        .stageVars(stageVars))
                .chatId(chatId);
    }

    @Test
    @DisplayName("Test mapping: message")
    @SneakyThrows
    void testMapToData_0() {
        var update = getUpdate(Type.MESSAGE);
        var sdkMapper = new TgSdkUpdateToDataMapper(clientRepository, clientEntityToDataMapper, initialStageName);
        var expectedUpdateData = prefilledUpdateData
                .message(new MessageData()
                        .id(messageId)
                        .text(messageText)
                )
                .type(Type.MESSAGE);

        var actualUpdateData = sdkMapper.map(update);

        assertEquals(expectedUpdateData, actualUpdateData);
    }

    @Test
    @DisplayName("Test mapping: callback")
    @SneakyThrows
    void testMapToData_1() {
        var update = getUpdate(Type.CALLBACK);
        var sdkMapper = new TgSdkUpdateToDataMapper(clientRepository, clientEntityToDataMapper, initialStageName);
        var expectedUpdateData = prefilledUpdateData
                .callback(
                        new CallbackData()
                                .data(callbackData)
                )
                .type(Type.CALLBACK);

        var actualUpdateData = sdkMapper.map(update);

        assertEquals(expectedUpdateData, actualUpdateData);
    }

    @Test
    @DisplayName("Test mapping: unknown type")
    @SneakyThrows
    void testMapToData_2() {
        var update = getUpdate(null);
        var sdkMapper = new TgSdkUpdateToDataMapper(clientRepository, clientEntityToDataMapper, initialStageName);

        var exception = assertThrows(MappingException.class, () -> sdkMapper.map(update));

        assertEquals("Unknown update type", exception.getMessage());
    }

    @Test
    @DisplayName("Test mapping: user not found")
    @SneakyThrows
    void testMapToData_3() {
        when(clientRepository.findByExternalId(externalId))
            .thenThrow(ClientNotFoundException.class);
        var update = getUpdate(Type.MESSAGE);
        var sdkMapper = new TgSdkUpdateToDataMapper(clientRepository, clientEntityToDataMapper, initialStageName);
        var expectedUpdateData = new UpdateData()
                .client(new ClientData()
                        .externalId(externalId)
                        .name(userName)
                        .previousStages(List.of())
                        .bindNewStage(initialStageName)
                        .setStageInitiated()
                        .additionalVars(Map.of())
                        .stageVars(Map.of())
                )
                .chatId(chatId)
                .message(
                        new MessageData()
                                .id(messageId)
                                .text(messageText)
                )
                .type(Type.MESSAGE);

        var actualUpdateData = sdkMapper.map(update);

        assertEquals(expectedUpdateData, actualUpdateData);
    }

    private Update getUpdate(Type type) {
        var update = new Update();

        var user = new User(externalId, "Some", false);
        user.setUserName(userName);

        var chat = mock(Chat.class);
        when(chat.getId()).thenReturn(chatId);

        var message = new Message();
        message.setChat(chat);
        message.setText(messageText);
        message.setMessageId(messageId);

        switch (type) {
            case MESSAGE -> {
                update.setMessage(message);
                message.setFrom(user);
            }
            case CALLBACK -> {
                var callback = new CallbackQuery();
                callback.setData(callbackData);
                callback.setMessage(message);
                callback.setFrom(user);
                update.setCallbackQuery(callback);
            }
            case null -> update.setMessageReaction(new MessageReactionUpdated());
        }
        return update;
    }
}

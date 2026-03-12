package com.github.spector517.xtbot.core.application.handler;

import com.github.spector517.xtbot.core.application.config.*;
import com.github.spector517.xtbot.core.application.data.inbound.ChatMessage;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.MessageType;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputType;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.mapper.Mapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class EventHandlerTest {

    // Helper: create a BOT ChatMessage with a known telegramMessageId (sentAt=null for simplicity)
    private static ChatMessage botMessage(int id) {
        return new ChatMessage(id, null, null, MessageType.BOT);
    }

    private Gateway gateway;
    private Config config;

    private Mapper<Map<String, Object>, UpdateData> contextMapper;

    private String firstStageName;
    private String firstStageMessageText;
    private String firstStageButtonDisplayName;
    private String firstStageButtonData;
    private Stage firstStage;
    private String secondStageName;
    private String secondStageMessageText;
    private String failStageName;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        contextMapper = mock(Mapper.class);
        when(contextMapper.map(any(UpdateData.class))).thenReturn(Map.of());

        gateway = mock(Gateway.class);
        when(gateway.getContextMapper()).thenReturn(contextMapper);

        config = mock(Config.class);
        firstStageName = "stage1";
        firstStageMessageText = "stage1_message";
        firstStageButtonDisplayName = "stage1_button_name";
        firstStageButtonData = "stage1_button_data";
        secondStageName = "stage2";
        secondStageMessageText = "stage2_message";
        failStageName = "fail_stage";

        var firstStageMessageTemplate = mock(Template.class);
        var firstStageButtonDisplayNameTemplate = mock(Template.class);
        var firstStageButtonDataTemplate = mock(Template.class);
        when(firstStageButtonDisplayNameTemplate.value(anyMap())).thenReturn(firstStageButtonDisplayName);
        when(firstStageButtonDataTemplate.value(anyMap())).thenReturn(firstStageButtonData);
        when(firstStageMessageTemplate.value(anyMap())).thenReturn(firstStageMessageText);
        var secondStageTemplate = mock(Template.class);
        when(secondStageTemplate.value(anyMap())).thenReturn(secondStageName);
        var firstStageMessage = mock(Message.class);
        when(firstStageMessage.text()).thenReturn(Optional.of(firstStageMessageTemplate));
        when(firstStageMessage.parseMode()).thenReturn(ParseMode.MARKDOWN);
        var button = mock(Button.class);
        when(button.display()).thenReturn(firstStageButtonDisplayNameTemplate);
        when(button.data()).thenReturn(firstStageButtonDataTemplate);
        when(firstStageMessage.buttons()).thenReturn(List.of(List.of(button)));
        firstStage = mock(Stage.class);
        when(firstStage.message()).thenReturn(Optional.of(firstStageMessage));
        when(firstStage.next()).thenReturn(Optional.of(secondStageTemplate));
        when(firstStage.name()).thenReturn(firstStageName);
        when(firstStage.removeButtons()).thenReturn(true);
        when(firstStage.sendTyping()).thenReturn(true);
        when(firstStage.getAdditionalVars(anyMap())).thenReturn(Map.of("key1", "val1"));
        when(config.getStage(firstStageName)).thenReturn(firstStage);

        var secondStageMessageTemplate = mock(Template.class);
        when(secondStageMessageTemplate.value(anyMap())).thenReturn(secondStageMessageText);
        var secondStageMessage = mock(Message.class);
        when(secondStageMessage.text()).thenReturn(Optional.of(secondStageMessageTemplate));
        when(secondStageMessage.parseMode()).thenReturn(ParseMode.PLAIN_TEXT);
        when(secondStageMessage.buttons()).thenReturn(List.of());
        var secondStage = mock(Stage.class);
        when(secondStage.message()).thenReturn(Optional.of(secondStageMessage));
        when(secondStage.next()).thenReturn(Optional.empty());
        when(secondStage.name()).thenReturn(secondStageName);
        when(config.getStage(secondStageName)).thenReturn(secondStage);

        var failStage = mock(Stage.class);
        when(failStage.message()).thenReturn(Optional.empty());
        when(failStage.name()).thenReturn(failStageName);
        when(config.failStage()).thenReturn(failStage);
    }

    @Test
    @DisplayName("Not initiated")
    @SneakyThrows
    void run_0() {
        var clientData = new ClientData()
            .previousStages(List.of())
            .bindNewStage(firstStageName)
            .messages(List.of(botMessage(111)));
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).thenReturn(Optional.of(1111));

        new EventHandler(config, gateway, updateData).run();

        verify(contextMapper, times(1)).map(any(UpdateData.class));
        assertEquals(1, outputCaptor.getAllValues().size());
        assertEquals(
                new OutputData(11L, OutputType.SEND_MESSAGE)
                        .text(firstStageMessageText)
                        .parseMode(ParseMode.MARKDOWN.type())
                        .buttons(List.of(List.of(
                                new OutputData.Button(firstStageButtonDisplayName, firstStageButtonData)
                        ))),
                outputCaptor.getValue()
        );
        assertEquals(1111, clientData.getPreviousSentMessageId().orElseThrow());
        assertTrue(clientData.stageInitiated());
    }

    @Test
    @DisplayName("Initiated, not completed, next stage not defined")
    @SneakyThrows
    void run_1() {
        var clientData = new ClientData()
            .previousStages(List.of())
            .messages(List.of(botMessage(111)))
            .bindNewStage(firstStageName)
            .setStageInitiated()
            .additionalVars(Map.of());
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).thenReturn(Optional.empty());
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(true);
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        when(firstStage.actions()).thenReturn(List.of());
        when(firstStage.next()).thenReturn(Optional.empty());

        new EventHandler(config, gateway, updateData).run();

        verify(contextMapper, times(2)).map(any(UpdateData.class));
        assertEquals(OutputType.TYPING, outputCaptor.getValue().type());
        assertEquals(1, outputCaptor.getAllValues().size());
        assertEquals(Map.of("key1", "val1"), clientData.additionalVars());
        assertTrue(clientData.stageCompleted());
    }

    @Test
    @DisplayName("Initiated, not completed, update not accepted")
    @SneakyThrows
    void run_2() {
        var clientData = new ClientData()
            .previousStages(List.of())
            .bindNewStage(firstStageName)
            .setStageInitiated();
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(false);

        new EventHandler(config, gateway, updateData).run();

        verify(gateway, never()).produce(any(OutputData.class));
        verify(contextMapper).map(any(UpdateData.class));
    }

    @Test
    @DisplayName("Initiated, not completed, next stage is defined")
    @SneakyThrows
    void run_3() {
        var clientData = new ClientData()
            .previousStages(List.of())
            .messages(List.of(botMessage(111)))
            .bindNewStage(firstStageName)
            .setStageInitiated()
            .additionalVars(Map.of("key2", "val2"));
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(true);
        var action = mock(Action.class);
        when(action.execute(any(Map.class))).thenReturn(true);
        when(action.register()).thenReturn("register");
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        when(firstStage.actions()).thenReturn(List.of(action));
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).then(invocationOnMock -> {
            var output = (OutputData) invocationOnMock.getArgument(0);
            return output.type() == OutputType.SEND_MESSAGE ? Optional.of(1111) : Optional.empty();
        });

        new EventHandler(config, gateway, updateData).run();

        verify(contextMapper, times(2 + 1 + 1)).map(any(UpdateData.class));
        var capturedValues = outputCaptor.getAllValues();
        assertEquals(2 + 2, capturedValues.size());
        assertEquals(new OutputData(11L, OutputType.TYPING), capturedValues.getFirst());
        assertEquals(new OutputData(11L, OutputType.TYPING), capturedValues.get(1));
        assertEquals(
                new OutputData(11L, OutputType.EDIT_MESSAGE)
                        .messageId(111)
                        .buttons(List.of()),
                capturedValues.get(2)
        );
        assertEquals(
                new OutputData(11L, OutputType.SEND_MESSAGE)
                        .text(secondStageMessageText)
                        .parseMode(ParseMode.PLAIN_TEXT.type())
                        .buttons(List.of()),
                capturedValues.get(3)
        );
        assertEquals(secondStageName, clientData.stageName());
        assertTrue(clientData.stageInitiated());
        assertEquals(1111, clientData.getPreviousSentMessageId().orElseThrow());
        assertEquals(clientData.stageVars(), Map.of("register", true));
        assertEquals(List.of(firstStage.name()), clientData.previousStages());
        assertEquals(Map.of("key2", "val2", "key1", "val1"), clientData.additionalVars());
    }

    @Test
    @DisplayName("Initiated, error while completing")
    @SneakyThrows
    void run_4() {
        var clientData = new ClientData()
            .previousStages(List.of())
            .messages(List.of(botMessage(111)))
            .bindNewStage(firstStageName)
            .setStageInitiated();
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class)))
            .thenThrow(new AcceptorExecutionException(new Exception()));
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).then(invocationOnMock -> {
            var output = (OutputData) invocationOnMock.getArgument(0);
            return output.type() == OutputType.SEND_MESSAGE ? Optional.of(1111) : Optional.empty();
        });

        new EventHandler(config, gateway, updateData).run();

        verify(contextMapper, times(1 + 1)).map(any(UpdateData.class));
        assertEquals(1, outputCaptor.getAllValues().size());
        assertEquals(
                new OutputData(11L, OutputType.EDIT_MESSAGE)
                        .messageId(111)
                        .buttons(List.of()),
                outputCaptor.getValue()
        );
        assertEquals(111, clientData.getPreviousSentMessageId().orElseThrow());
        assertEquals(clientData.stageName(), failStageName);
        assertTrue(clientData.stageInitiated());
    }

    @Test
    @DisplayName("Not initiated, autocomplete, no typing")
    @SneakyThrows
    void run_5() {
        var clientData = new ClientData()
            .previousStages(List.of())
            .bindNewStage(firstStageName)
            .additionalVars(Map.of());
        var updateData = new UpdateData()
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(false);
        when(firstStage.autocomplete()).thenReturn(true);
        when(firstStage.actions()).thenReturn(List.of());
        when(firstStage.sendTyping()).thenReturn(false);
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).thenReturn(Optional.of(1111));

        new EventHandler(config, gateway, updateData).run();

        verify(gateway, times(3)).produce(any(OutputData.class));
        var capturedValues = outputCaptor.getAllValues();
        assertEquals(OutputType.SEND_MESSAGE, capturedValues.getFirst().type());
        assertEquals(OutputType.EDIT_MESSAGE, capturedValues.get(1).type());
        assertEquals(OutputType.SEND_MESSAGE, capturedValues.get(2).type());
        assertEquals(secondStageName, clientData.stageName());
        assertTrue(clientData.stageInitiated());
        assertFalse(clientData.stageCompleted());
    }
}

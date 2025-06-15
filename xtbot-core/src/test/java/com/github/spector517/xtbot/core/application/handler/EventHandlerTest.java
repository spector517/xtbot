package com.github.spector517.xtbot.core.application.handler;

import com.github.spector517.xtbot.core.application.config.*;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class EventHandlerTest {
    
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
            .currentStage(firstStageName)
            .previousStages(List.of())
            .previousSendedMessageId(111);
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).thenReturn(1111);
        var expectedOutput = new OutputData()
            .chatId(11)
            .text(firstStageMessageText)
            .parseMode(ParseMode.MARKDOWN.type())
            .buttons(List.of(List.of(
                new OutputData.Button()
                    .display(firstStageButtonDisplayName)
                    .data(firstStageButtonData)
            )))
            .previousSendedMessageId(111);

        new EventHandler(config, gateway, updateData).call();

        verify(contextMapper, times(2)).map(any(UpdateData.class));
        assertEquals(expectedOutput, outputCaptor.getValue());
        verify(gateway, times(1 + 1)).produce(any(OutputData.class));
        assertEquals(1111, clientData.previousSendedMessageId());
        assertTrue(clientData.currentStageInitiated());
    }

    @Test
    @DisplayName("Initiated, not completed, next stage not defined")
    @SneakyThrows
    void run_1() {
        var clientData = new ClientData()
            .currentStage(firstStageName)
            .currentStageInitiated(true)
            .previousStages(List.of())
            .previousSendedMessageId(111)
            .additionalVars(Map.of());
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(true);
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        when(firstStage.actions()).thenReturn(List.of());
        when(firstStage.next()).thenReturn(Optional.empty());

        new EventHandler(config, gateway, updateData).call();

        verify(contextMapper, times(3)).map(any(UpdateData.class));
        verify(gateway, never()).produce(any(OutputData.class));
        assertEquals(Map.of("key1", "val1"), clientData.additionalVars());
        assertTrue(clientData.currentStageCompleted());
    }

    @Test
    @DisplayName("Initiated, not completed, update not accepted")
    @SneakyThrows
    void run_2() {
        var clientData = new ClientData()
            .currentStage(firstStageName)
            .currentStageInitiated(true)
            .previousStages(List.of());
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(false);

        new EventHandler(config, gateway, updateData).call();

        verify(gateway, never()).produce(any(OutputData.class));
        verify(contextMapper).map(any(UpdateData.class));
    }

    @Test
    @DisplayName("Initiated, not completed, next stage is defined")
    @SneakyThrows
    void run_3() {
        var clientData = new ClientData()
            .currentStage(firstStageName)
            .currentStageInitiated(true)
            .previousStages(List.of())
            .previousSendedMessageId(111)
            .additionalVars(Map.of("key2", "val2"));
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(true);
        var action = mock(Action.class);
        when(action.execute(any(UpdateData.class))).thenReturn(true);
        when(action.register()).thenReturn("register");
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        when(firstStage.actions()).thenReturn(List.of(action));
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).thenReturn(1111);
        var expectedOutput = new OutputData()
            .chatId(11)
            .text(secondStageMessageText)
            .parseMode(ParseMode.PLAIN_TEXT.type())
            .removeButtons(true)
            .previousSendedMessageId(111);

        new EventHandler(config, gateway, updateData).call();

        verify(contextMapper, times(3 + 2)).map(any(UpdateData.class));
        verify(gateway, times(1 + 1)).produce(any(OutputData.class));
        assertEquals(expectedOutput, outputCaptor.getValue());
        assertEquals(secondStageName, clientData.currentStage());
        assertTrue(clientData.currentStageInitiated());
        assertEquals(1111, clientData.previousSendedMessageId());
        assertEquals(clientData.stageVars(), Map.of("register", true));
        assertEquals(List.of(firstStage.name()), clientData.previousStages());
        assertEquals(Map.of("key2", "val2", "key1", "val1"), clientData.additionalVars());
    }

    @Test
    @DisplayName("Initiated, error while initiating")
    @SneakyThrows
    void run_4() {
        var clientData = new ClientData()
            .currentStage(firstStageName)
            .currentStageInitiated(true)
            .previousStages(List.of())
            .previousSendedMessageId(111);
        var updateData = new UpdateData()
            .chatId(11)
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class)))
            .thenThrow(new AcceptorExecutionException(new Exception()));
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).thenReturn(0);
        var expectedOutput = new OutputData()
            .chatId(11)
            .removeButtons(true)
            .previousSendedMessageId(111);

        new EventHandler(config, gateway, updateData).call();

        verify(contextMapper, times(1 + 2)).map(any(UpdateData.class));
        assertEquals(expectedOutput, outputCaptor.getValue());
        verify(gateway, times(1 + 1)).produce(any(OutputData.class));
        assertEquals(111, clientData.previousSendedMessageId());
        assertEquals(clientData.currentStage(), failStageName);
        assertTrue(clientData.currentStageInitiated());
    }

    @Test
    @DisplayName("Not initiated, autocomplete")
    @SneakyThrows
    void run_5() {
        var clientData = new ClientData()
            .currentStage(firstStageName)
            .previousStages(List.of())
            .additionalVars(Map.of());
        var updateData = new UpdateData()
            .client(clientData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(false);
        when(firstStage.autocomplete()).thenReturn(true);
        when(firstStage.actions()).thenReturn(List.of());

        new EventHandler(config, gateway, updateData).call();

        verify(gateway, times(2 + 2)).produce(any(OutputData.class));
        assertEquals(secondStageName, clientData.currentStage());
        assertTrue(clientData.currentStageInitiated());
        assertFalse(clientData.currentStageCompleted());
    }
}

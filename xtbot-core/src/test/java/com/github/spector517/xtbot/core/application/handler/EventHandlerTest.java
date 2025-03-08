package com.github.spector517.xtbot.core.application.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.config.Acceptor;
import com.github.spector517.xtbot.core.application.config.AcceptorExecutionException;
import com.github.spector517.xtbot.core.application.config.Action;
import com.github.spector517.xtbot.core.application.config.Button;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.config.Message;
import com.github.spector517.xtbot.core.application.config.ParseMode;
import com.github.spector517.xtbot.core.application.config.Stage;
import com.github.spector517.xtbot.core.application.config.Template;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.repository.ClientEntity;
import com.github.spector517.xtbot.core.application.repository.ClientRepository;

import lombok.SneakyThrows;

@SuppressWarnings("unchecked")
class EventHandlerTest {
    
    private ComponentsContainer componentsContainer;
    private Gateway<Object> gateway;
    private Config config;

    private Mapper<Object, UpdateData> sdkMapper;
    private Mapper<UpdateData, Map<String, Object>> contextMapper;
    private Mapper<ClientData, ClientEntity> entityMapper;
    private ClientRepository repository;

    private String firstStageName;
    private String firstStageMessageText;
    private String firstStageButtonDisplayName;
    private String firstStageButtonData;
    private Stage firstStage;
    private String secondStageName;
    private String secondStageMessageText;
    private String failStageName;
    private Stage secondStage;
    private Stage failStage;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        componentsContainer = mock(ComponentsContainer.class);
        
        sdkMapper = mock(Mapper.class);
        contextMapper = mock(Mapper.class);
        when(contextMapper.map(any(UpdateData.class))).thenReturn(Map.of());
        entityMapper = mock(Mapper.class);
        repository = mock(ClientRepository.class);
        when(componentsContainer.tgSdkUpdateToDataMapper()).thenReturn(sdkMapper);
        when(componentsContainer.updateDataToContextMapper()).thenReturn(contextMapper);
        when(componentsContainer.clientDataToEntityMapper()).thenReturn(entityMapper);
        when(componentsContainer.clientRepository()).thenReturn(repository);

        gateway = mock(Gateway.class);
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
        secondStage = mock(Stage.class);
        when(secondStage.message()).thenReturn(Optional.of(secondStageMessage));
        when(secondStage.next()).thenReturn(Optional.empty());
        when(secondStage.name()).thenReturn(secondStageName);
        when(config.getStage(secondStageName)).thenReturn(secondStage);

        failStage = mock(Stage.class);
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
        var update = new Object();
        when(sdkMapper.map(update)).thenReturn(updateData);
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

        new TestEventHandler(update, config, componentsContainer, gateway).run();

        verify(contextMapper, times(2)).map(any(UpdateData.class));
        assertEquals(expectedOutput, outputCaptor.getValue());
        verify(repository).save(any());
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
        var update = new Object();
        when(sdkMapper.map(update)).thenReturn(updateData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(true);
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        when(firstStage.actions()).thenReturn(List.of());
        when(firstStage.next()).thenReturn(Optional.empty());

        new TestEventHandler(update, config, componentsContainer, gateway).run();

        verify(contextMapper, times(3)).map(any(UpdateData.class));
        verify(repository).save(any());
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
        var update = new Object();
        when(sdkMapper.map(update)).thenReturn(updateData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(false);

        new TestEventHandler(update, config, componentsContainer, gateway).run();

        verify(gateway, never()).produce(any(OutputData.class));
        verify(contextMapper).map(any(UpdateData.class));
        verify(repository, never()).save(any());
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
        var update = new Object();
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(true);
        var action = mock(Action.class);
        when(action.execute(any(UpdateData.class))).thenReturn(true);
        when(action.register()).thenReturn("register");
        when(sdkMapper.map(update)).thenReturn(updateData);
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

        new TestEventHandler(update, config, componentsContainer, gateway).run();

        verify(contextMapper, times(3 + 2)).map(any(UpdateData.class));
        verify(repository, times(2)).save(any());
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
        var update = new Object();
        when(sdkMapper.map(update)).thenReturn(updateData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class)))
            .thenThrow(new AcceptorExecutionException(new Exception()));
        when(firstStage.acceptors()).thenReturn(List.of(acceptor));
        var outputCaptor = ArgumentCaptor.forClass(OutputData.class);
        when(gateway.produce(outputCaptor.capture())).thenReturn(0);
        var expectedOutput = new OutputData()
            .chatId(11)
            .previousSendedMessageId(111);

        new TestEventHandler(update, config, componentsContainer, gateway).run();

        verify(contextMapper, times(1 + 2)).map(any(UpdateData.class));
        assertEquals(expectedOutput, outputCaptor.getValue());
        verify(repository).save(any());
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
        var update = new Object();
        when(sdkMapper.map(update)).thenReturn(updateData);
        var acceptor = mock(Acceptor.class);
        when(acceptor.accept(any(UpdateData.class))).thenReturn(false);
        when(firstStage.autocomplete()).thenReturn(true);
        when(firstStage.actions()).thenReturn(List.of());

        new TestEventHandler(update, config, componentsContainer, gateway).run();

        verify(gateway, times(2 + 2)).produce(any(OutputData.class));
        assertEquals(secondStageName, clientData.currentStage());
        assertTrue(clientData.currentStageInitiated());
        assertFalse(clientData.currentStageCompleted());
    }
}

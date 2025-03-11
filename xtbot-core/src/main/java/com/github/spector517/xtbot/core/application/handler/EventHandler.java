package com.github.spector517.xtbot.core.application.handler;

import java.util.HashMap;
import java.util.Map;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.config.Stage;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.gateway.GatewayException;
import com.github.spector517.xtbot.core.application.logger.MDCLogManager;
import com.github.spector517.xtbot.core.application.mapper.MappingException;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class EventHandler<T> implements Runnable {

    private final T updateEvent;
    private final Config config;
    private final ComponentsContainer container;
    private final Gateway<T> gateway;

    private UpdateData updateData;
    private Map<String, Object> context;
    private Stage stage;

    @Override
    @SneakyThrows
    public void run() {
        try {
            updateData = container.tgSdkUpdateToDataMapper().map(updateEvent);
            stage = config.getStage(updateData.client().currentStage());
            updateMDC();
        } catch (Exception ex) {
            logException(ex);
            clearMDC();
            throw ex;
        }

        log.info("Start event processing...");

        try {
            process();
        } catch(Exception ex) {
            logException(ex);
            try {
                bindFailStage();
                process();
            } catch(Exception e) {
                logException(ex);
                clearMDC();
                throw ex;
            }
        }

        log.info("Event processed.");
        clearMDC();
    }

    private void process() throws GatewayException, MappingException {
        if (!updateData.client().currentStageInitiated()) {
            initiateStage();
            if (stage.autocomplete()) {
                process();
            }
            return;
        }
        if (!updateData.client().currentStageCompleted()) {
            completeStage();
            if (updateData.client().currentStageCompleted() && bindNextStage()) {
                process();
            }
        }
    }

    private void initiateStage() throws GatewayException, MappingException {
        log.info("Initiating stage...");
        sendTyping();
        updateContext();
        var output = new OutputData().chatId(updateData.chatId());

        stage.message().ifPresent(message -> {
            message.id().ifPresent(id -> output.messageId(Integer.parseInt(id.value(context))));
            message.text().ifPresent(text -> output.text(text.value(context)));
            output.parseMode(message.parseMode().type());

            var buttons = message.buttons().stream().map(row -> 
                row.stream().map(button -> 
                    new OutputData.Button()
                        .display(button.display().value(context))
                        .data(button.data().value(context))
                ).toList()
            ).toList();
            output.buttons(buttons);
        });
        
        updateData.client().getPreviousStage().ifPresent(stageName -> {
            var previousStage = config.getStage(stageName);
            output.removeButtons(
                previousStage.message().isPresent() 
                    && !previousStage.message().get().buttons().isEmpty()
                    && previousStage.removeButtons()
            );
        });
        output.previousSendedMessageId(updateData.client().previousSendedMessageId());

        var messageId = gateway.produce(output);
        
        if (messageId != 0) {
            updateData.client().previousSendedMessageId(messageId);
        }
        updateData.client().currentStageInitiated(true);
        updateContext();
        saveToRepo();
        log.info("Stage initiated.");
    }

    private void sendTyping() throws GatewayException {
        gateway.produce(
            new OutputData()
                .chatId(updateData.chatId())
                .sendTyping(true)
        );
    }

    private void completeStage() throws MappingException {
        updateContext();
        var isNotAccepted = stage.acceptors().stream().noneMatch(acceptor -> {
            log.debug("Run acceptor: {}", acceptor.name());
            var res = acceptor.accept(updateData);
            log.debug("Acceptor '{}' result: {}", acceptor.name(), res);
            return res;
        });
        if (isNotAccepted&& !stage.autocomplete()) {
            log.warn("Update not accepted. Skipped.");
            return;
        }
        log.info("Completing stage...");

        updateData.client().stageVars(new HashMap<>());
        stage.actions().forEach(action -> {
            log.debug("Run action: {}", action.name());
            var result = action.execute(updateData);
            log.debug("Action '{}' result: {}", action.name(), result);
            var resultVar = action.register().isBlank() ? "_" : action.register();
            log.debug("Register action result to var '{}'", resultVar);
            updateData.client().stageVars().put(resultVar, result);
        });
        bindAdditionalVars();

        updateData.client().currentStageCompleted(true);
        updateData.client().registerCompletedStage(stage.name());
        updateContext();
        saveToRepo();
        log.info("Stage completed.");
    }

    private void bindAdditionalVars() throws MappingException {
        updateContext();
        var alreadyExistingAdditionalVars = updateData.client().additionalVars();
        var currentStageAdditionalVars = stage.getAdditionalVars(context);
        var allAdditionalVars = new HashMap<>(alreadyExistingAdditionalVars);
        allAdditionalVars.putAll(currentStageAdditionalVars);
        updateData.client().additionalVars(allAdditionalVars);
    }

    private boolean bindNextStage() {
        log.info("Binding next stage");
        Stage nextStage;
        try {
            if (stage.next().isEmpty()) {
                log.warn("Next stage is not defined");
                return false;
            }
            nextStage = config.getStage(stage.next().get().value(context));
        } catch(Exception ex) {
            nextStage = config.failStage();
        }
        stage = nextStage;
        updateData.client().currentStageInitiated(false);
        updateData.client().currentStageCompleted(false);
        updateData.client().currentStage(nextStage.name());

        log.info("Next stage is '{}'", stage.name());
        return true;
    }

    private void bindFailStage() {
        log.info("Binding fail stage");
        stage = config.failStage();

        updateData.client().currentStageInitiated(false);
        updateData.client().currentStageCompleted(false);
        updateData.client().currentStage(stage.name());

        log.info("Fail stage bound");
    }

    private void updateContext() throws MappingException {
        context = container.updateDataToContextMapper().map(updateData);
        updateMDC();
    }

    private void saveToRepo() throws MappingException {
        var entity = container.clientDataToEntityMapper().map(updateData.client());
        container.clientRepository().save(entity);
    }

    private void updateMDC() {
        MDCLogManager.put(updateData.client());
    }

    private void clearMDC() {
        MDCLogManager.clear();
    }

    private void logException(Exception ex) {
        log.error(ex.getMessage());
        log.debug("Exception occurred", ex);
    }
}

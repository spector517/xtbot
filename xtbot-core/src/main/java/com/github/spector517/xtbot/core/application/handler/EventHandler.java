package com.github.spector517.xtbot.core.application.handler;

import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.config.Stage;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.gateway.GatewayException;
import com.github.spector517.xtbot.core.mapper.MappingException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class EventHandler implements Runnable {

    private final Config config;
    private final Gateway gateway;
    private final UpdateData updateData;

    private Map<String, Object> context;
    private Stage stage;

    @Override
    @SneakyThrows
    public void run() {
        stage = config.getStage(updateData.client().stageName());
        try {
            process();
        } catch(Exception ex) {
            log.warn("Event processing failed: {}", ex.getMessage());
            log.warn("Trying to bind fail stage...");
            bindFailStage();
            process();
        }
    }

    private void process() throws GatewayException, MappingException {
        if (!updateData.client().stageInitiated()) {
            initiateStage();
            if (stage.autocomplete()) {
                process();
            }
            return;
        }
        if (!updateData.client().stageCompleted()) {
            completeStage();
            if (updateData.client().stageCompleted() && bindNextStage()) {
                process();
            }
        }
    }

    private void initiateStage() throws GatewayException, MappingException {
        log.debug("Initiating stage...");
        sendTyping();
        updateContext();
        var output = new OutputData().chatId(updateData.chatId());

        stage.message().ifPresent(message -> {
            message.id().ifPresent(id -> output.messageId(Integer.parseInt(id.value(context))));
            message.deleteId().ifPresent(deleteId -> 
                output.deleteMessageId(Integer.parseInt(deleteId.value(context)))
            );
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
        updateData.client().getPreviousSentMessageId().ifPresent(output::previousSentMessageId);

        var messageId = gateway.produce(output);
        
        if (messageId != 0) {
            updateData.client().registerSentMessageId(messageId);
        }
        updateData.client().setStageInitiated();
        updateContext();
        log.debug("Stage initiated.");
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
        log.debug("Completing stage...");

        updateData.client().stageVars(new HashMap<>());
        stage.actions().forEach(action -> {
            log.debug("Run action: {}", action.name());
            var result = action.execute(updateData);
            log.debug("Action '{}' result: {}", action.name(), result);
            var resultVar = action.register().isBlank() ? "_" : action.register();
            log.debug("Register action result to var '{}'", resultVar);
            updateData.client().updateStageVars(Map.of(resultVar, result));
        });
        bindAdditionalVars();

        updateData.client().setStageCompleted();
        updateContext();
        log.debug("Stage completed.");
    }

    private void bindAdditionalVars() throws MappingException {
        updateContext();
        var currentStageAdditionalVars = stage.getAdditionalVars(context);
        updateData.client().updateAdditionalVars(currentStageAdditionalVars);
    }

    private boolean bindNextStage() {
        log.debug("Binding next stage");
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
        log.debug("Next stage is '{}'", stage.name());
        updateData.client().bindNewStage(stage.name());
        return true;
    }

    private void bindFailStage() {
        var previousStageOptional = updateData.client().getPreviousStage();
        if (previousStageOptional.isEmpty() || !previousStageOptional.get().equals(stage.name())) {
            updateData.client().bindNewStage(stage.name());
        }
        log.debug("Binding fail stage");
        stage = config.failStage();
        updateData.client().bindNewStage(stage.name());

        log.debug("Fail stage bound");
    }

    private void updateContext() throws MappingException {
        context = gateway.getContextMapper().map(updateData);
    }
}

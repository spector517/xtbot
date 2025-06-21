package com.github.spector517.xtbot.core.application.handler;

import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.config.Stage;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputType;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.gateway.GatewayException;
import com.github.spector517.xtbot.core.mapper.MappingException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        gateway.produce(new OutputData(updateData.chatId(), OutputType.TYPING));
        updateContext();

        var previousStageName = updateData.client().getPreviousStage();
        if (previousStageName.isPresent()) {
            var previousStage = config.getStage(previousStageName.get());
            if (
                    previousStage.message().isPresent()
                            && !previousStage.message().get().buttons().isEmpty()
                            && previousStage.removeButtons()
                            && updateData.client().getPreviousSentMessageId().isPresent()
            ) {
                var output = new OutputData(updateData.chatId(), OutputType.EDIT_MESSAGE);
                output.messageId(updateData.client().getPreviousSentMessageId().get());
                output.buttons(List.of());
                gateway.produce(output);
            }
        }

        Optional<Integer> sentMessageId = Optional.empty();
        var message = stage.message();
        if (message.isPresent()) {
            var text = message.get().text();
            if (text.isPresent()) {
                var output = message.get().id().isPresent()
                        ? new OutputData(updateData.chatId(), OutputType.EDIT_MESSAGE)
                            .messageId(Integer.parseInt(message.get().id().get().value(context)))
                        : new OutputData(updateData.chatId(), OutputType.SEND_MESSAGE);
                output.text(text.get().value(context));
                output.parseMode(message.get().parseMode().type());
                var buttons = message.get().buttons().stream().map(row ->
                        row.stream().map(button ->
                                new OutputData.Button(button.display().value(context), button.data().value(context))
                        ).toList()
                ).toList();
                output.buttons(buttons);
                sentMessageId = gateway.produce(output);
            }

            var deleteId = message.get().deleteId();
            if (deleteId.isPresent()) {
                var output = new OutputData(updateData.chatId(), OutputType.DELETE_MESSAGE);
                output.deleteMessageId(Integer.parseInt(deleteId.get().value(context)));
                gateway.produce(output);
            }
        }

        sentMessageId.ifPresent(id -> updateData.client().registerSentMessageId(id));
        updateData.client().setStageInitiated();
        log.debug("Stage initiated.");
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
        for (var action : stage.actions()) {
            log.debug("Run action: {}", action.name());
            var result = action.execute(context);
            log.debug("Action '{}' result: {}", action.name(), result);
            var resultVar = action.register();
            log.debug("Register action result to var '{}'", resultVar);
            updateData.client().updateStageVars(Map.of(resultVar, result));
            updateContext();
        }
        var currentStageAdditionalVars = stage.getAdditionalVars(context);
        updateData.client().updateAdditionalVars(currentStageAdditionalVars);

        updateData.client().setStageCompleted();
        log.debug("Stage completed.");
    }

    private boolean bindNextStage() throws MappingException {
        log.debug("Binding next stage");
        updateContext();
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

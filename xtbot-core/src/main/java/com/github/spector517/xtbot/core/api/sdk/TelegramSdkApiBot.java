package com.github.spector517.xtbot.core.api.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.telegram.telegrambots.meta.api.objects.Update;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.gateway.GatewayException;
import com.github.spector517.xtbot.core.application.logger.MDCLogManager;
import com.github.spector517.xtbot.core.application.mapper.MappingException;
import com.github.spector517.xtbot.core.mapper.TgSdkUpdateToDataMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TelegramSdkApiBot extends ExtendedTelegramLongPollingBot {

    private final Config config;
    private final ComponentsContainer container;

    private final ExecutorService executorService;
    private final Map<Long, Future<?>> inProgressEvents;

    public TelegramSdkApiBot(Config config) {
        super(config.botToken());
        this.config = config;
        this.container = config.container();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.inProgressEvents = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            consume(update);
        } catch (GatewayException ex) {
            if (ex.getCause() instanceof MappingException mappingException) {
                log.warn(mappingException.getMessage());
                return;
            }
            logException(ex);
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return container.botUsername();
    }

    @Override
    public void consume(Update update) throws GatewayException {

        final long clientId;
        try {
            clientId = TgSdkUpdateToDataMapper.getClientId(update);
            log.info("Received event");
        } catch (MappingException ex) {
            throw new GatewayException(ex);
        }
        pruneInProgressEvents();

        if (inProgressEvents.containsKey(clientId)) {
            log.warn("Client has uncompleted events. Skipping.");
            return;
        }

        var handler = new TelegramSdkEventHandler(update, config, container, this);
        log.info("Submitting event");
        var future = executorService.submit(handler);
        inProgressEvents.put(clientId, future);

        MDCLogManager.clear();
    }

    @Override
    public int produce(OutputData outputData) throws GatewayException {
        if (outputData.sendTyping()) {
            sendTyping(outputData);
        }
        
        if (outputData.removeButtons()) {
            removeButtons(outputData);
        }

        if (outputData.deleteMessageId() > 0) {
            deleteMessage(outputData);
        }

        if (outputData.messageId() > 0) {
            return editMessage(outputData);
        }

        if (outputData.text() != null) {
            return sendMessage(outputData);
        }

        return 0;
    }

    private int editMessage(OutputData outputData) throws GatewayException {
        if (outputData.text() != null) {
            return editTextMessage(outputData);
        }
        if (outputData.buttons() != null) {
            return editButtons(outputData);
        }
        return 0;
    }

    private void pruneInProgressEvents() {
        inProgressEvents.entrySet().removeIf(entry -> entry.getValue().isDone());
    }

    private void logException(Exception ex) {
        log.error(ex.getMessage());
        log.debug("Exception occurred", ex);
    }
}

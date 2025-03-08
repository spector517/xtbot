package com.github.spector517.xtbot.core.api.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.gateway.GatewayException;
import com.github.spector517.xtbot.core.application.logger.MDCLogManager;
import com.github.spector517.xtbot.core.application.mapper.MappingException;
import com.github.spector517.xtbot.core.mapper.TgSdkUpdateToDataMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TelegramSdkApiBot extends TelegramLongPollingBot implements Gateway<Update> {

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
            MDCLogManager.putClientId(clientId);
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
        if (outputData.removeButtons()) {
            removeButtons(outputData);
        }

        if (outputData.text() != null) {
            return sendMessage(outputData);
        }

        if (outputData.sendTyping()) {
            sendTyping(outputData);
        }

        return 0;
    }

    private void sendTyping(OutputData outputData) throws GatewayException {
        var chatAction = SendChatAction.builder()
            .action(ActionType.TYPING.toString())
            .chatId(outputData.chatId())
            .build();
        try {
            execute(chatAction);
        } catch (TelegramApiException ex) {
            log.error("Sending typing action error");
            logException(ex);
            throw new GatewayException(ex);
        }
    }

    private void removeButtons(OutputData outputData) throws GatewayException {
        if (outputData.removeButtons()) {
            try {
                execute(EditMessageReplyMarkup.builder()
                    .chatId(outputData.chatId())
                    .messageId(outputData.previousSendedMessageId())
                    .build()
                );
            } catch (TelegramApiException ex) {
                log.error("Removing buttons error");
                logException(ex);
                throw new GatewayException(ex);
            }
        }
    }

    private int sendMessage(OutputData outputData) throws GatewayException {
        var sendMessage = SendMessage.builder()
            .text(outputData.text())
            .parseMode(outputData.parseMode())
            .chatId(outputData.chatId())
            .build();
        getInlineKeyboardMarkup(outputData).ifPresent(sendMessage::setReplyMarkup);
        sendMessage.setParseMode(outputData.parseMode());
        try {
            return execute(sendMessage).getMessageId();
        } catch (TelegramApiException ex) {
            log.error("Sending message error");
            logException(ex);
            throw new GatewayException(ex);
        }
    }

    private Optional<InlineKeyboardMarkup> getInlineKeyboardMarkup(OutputData outputData) {
        var buttons = outputData.buttons().stream().map(row ->
                row.stream().map(button ->
                    InlineKeyboardButton.builder()
                        .text(button.display())
                        .callbackData(button.data())
                        .build()
                ).toList()
            ).toList();
        if (!buttons.isEmpty()) {
            var markup = InlineKeyboardMarkup.builder()
                .keyboard(buttons)
                .build();
            return Optional.of(markup);
        }
        return Optional.empty();
    }

    private void pruneInProgressEvents() {
        inProgressEvents.entrySet().removeIf(entry -> entry.getValue().isDone());
    }

    private void logException(Exception ex) {
        log.error(ex.getMessage());
        log.debug("Exception occurred", ex);
    }
}

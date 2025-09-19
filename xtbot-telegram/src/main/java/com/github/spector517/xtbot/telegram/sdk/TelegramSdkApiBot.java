package com.github.spector517.xtbot.telegram.sdk;

import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.extension.CommonMethodsLoader;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.gateway.GatewayException;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.mapper.MappingException;
import com.github.spector517.xtbot.core.properties.data.Properties;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.telegram.mapper.TgSdkUpdateToDataMapper;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.telegram.telegrambots.client.AbstractTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Accessors(fluent = true)
public class TelegramSdkApiBot implements LongPollingSingleThreadUpdateConsumer, Gateway {

    public static final long SHUTDOWN_TIMEOUT_SECONDS = 20L;

    private final AbstractTelegramClient telegramClient;
    private final ExecutorService executorService;
    private final ClientRepository clientRepository;
    private final Properties properties;
    private final Mapper<UpdateData, org.telegram.telegrambots.meta.api.objects.Update> sdkMapper;
    private final Mapper<Map<String, Object>, UpdateData> contextMapper;
    private final Mapper<Update, UpdateData> apiMapper;
    private final Mapper<Object, Object> actionResultMapper;
    private final Mapper<ClientEntity, ClientData> toEntityMapper;
    private final Render render;
    private final CommonMethodsLoader commonMethodsLoader;
    @Getter
    private final String token;

    private final Map<Long, Future<?>> inProgressEvents;
    private final Config config;

    private boolean active;

    @Data
    @Accessors(fluent = true, chain = true)
    public static class Parameters {
        private AbstractTelegramClient telegramClient;
        private ExecutorService executorService;
        private ClientRepository clientRepository;
        private Properties properties;
        private Mapper<UpdateData, org.telegram.telegrambots.meta.api.objects.Update> sdkMapper;
        private Mapper<Map<String, Object>, UpdateData> contextMapper;
        private Mapper<Update, UpdateData> apiMapper;
        private Mapper<Object, Object> actionResultMapper;
        private Mapper<ClientEntity, ClientData> toEntityMapper;
        private Mapper<ClientData,ClientEntity> fromEntityMapper;
        private Render render;
        private CommonMethodsLoader commonMethodsLoader;
        private String token;
    }

    @SneakyThrows
    public TelegramSdkApiBot(Parameters params) {
        this.telegramClient = params.telegramClient;
        this.executorService = params.executorService;
        this.clientRepository = params.clientRepository;
        this.properties = params.properties;
        this.sdkMapper = params.sdkMapper;
        this.contextMapper = params.contextMapper;
        this.apiMapper = params.apiMapper;
        this.actionResultMapper = params.actionResultMapper;
        this.toEntityMapper = params.toEntityMapper;
        this.render = params.render;
        this.commonMethodsLoader = params.commonMethodsLoader;
        this.token = params.token;

        this.inProgressEvents = new ConcurrentHashMap<>();
        this.config = new Config(this);

        this.active = true;
    }

    @Override
    public synchronized void consume(List<org.telegram.telegrambots.meta.api.objects.Update> updates) {
        updates.forEach(this::consume);
    }

    @Override
    public synchronized void consume(org.telegram.telegrambots.meta.api.objects.Update update) {
        if (!active) {
            log.warn("Bot is shutting down. Skipping update.");
            return;
        }
        try {
            log.debug("Received update: {}", update);
            var clientId = TgSdkUpdateToDataMapper.getClientId(update);
            MDC.put(ClientData.EXTERNAL_ID_KEY, String.valueOf(clientId));
            if (inProgressEvents.containsKey(clientId)) {
                log.warn("Client has uncompleted events. Skipping update.");
                return;
            }

            var updateData = sdkMapper.map(update);
            var handler = consume(updateData, config);
            var wrappedHandler = wrapHandler(handler, updateData);
            log.info("Submitting event");
            var futureTask = executorService.submit(wrappedHandler);
            inProgressEvents.put(clientId, futureTask);

        } catch (RejectedExecutionException ex) {
            log.error(
                    "Event processing rejected. Executor service is shutting down or overloaded: {}",
                    ex.getMessage()
            );
        } catch (MappingException ex) {
            log.warn("Failed mapping update to data: {}", ex.getMessage());
        } catch (GatewayException ex) {
            log.error("Error while handler creation {}", ex.getMessage());
            log.debug("Stack trace:", ex);
        } finally {
            MDC.clear();
        }
    }

    @Override
    public Mapper<Map<String, Object>, UpdateData> getContextMapper() {
        return contextMapper;
    }

    @Override
    public Mapper<com.github.spector517.xtbot.api.dto.Update, UpdateData> getApiMapper() {
        return apiMapper;
    }

    @Override
    public Mapper<Object, Object> getActionResultMapper() {
        return actionResultMapper;
    }

    @Override
    public Render getRender() {
        return render;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public AcceptorLoader getAcceptorLoader() {
        return commonMethodsLoader;
    }

    public ExecutorLoader getExecutorLoader() {
        return (ExecutorLoader) getAcceptorLoader();
    }

    @Override
    public Optional<Integer> produce(OutputData outputData) throws GatewayException {
        return switch (outputData.type()) {
            case TYPING -> sendTyping(outputData.chatId());
            case SEND_MESSAGE -> sendMessage(
                    outputData.chatId(),
                    outputData.text(),
                    outputData.parseMode(),
                    getInlineKeyboardMarkup(outputData).orElse(null)
            );
            case EDIT_MESSAGE -> editMessage(
                    outputData.chatId(),
                    outputData.messageId(),
                    outputData.text(),
                    outputData.parseMode(),
                    getInlineKeyboardMarkup(outputData).orElse(null)
            );
            case DELETE_MESSAGE -> deleteMessage(outputData.chatId(), outputData.deleteMessageId());
        };
    }

    public synchronized void shutdown() {
        try {
            log.info("Shutting down...");
            active = false;
            executorService.shutdown();
            log.info("Waiting for tasks to complete.");
            var tasksCompleted = executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (tasksCompleted) {
                log.info("All tasks completed successfully.");
            } else {
                log.warn("Some tasks did not complete within the timeout. Forcing shutdown.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Shutdown interrupted: {}", e.getMessage());
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("{} while shutting down: {}", e.getClass().getName(), e.getMessage());
        }
    }

    private Runnable wrapHandler(Runnable handler, UpdateData updateData) {
        var mdsContext = MDC.getCopyOfContextMap();
        return () -> {
            Optional<ClientEntity> entity = Optional.empty();
            try {
                MDC.setContextMap(mdsContext);
                log.info("Starting event processing...");
                handler.run();
                entity = Optional.of(toEntityMapper.map(updateData.client()));
                log.info("Event processed successfully.");
            } catch (Exception ex) {
                log.error("Event processing failed. {}: {}", ex.getClass().getName(), ex.getMessage());
                log.debug("Stack trace:", ex);
            } finally {
                entity.ifPresent(clientRepository::save);
                inProgressEvents.remove(updateData.client().externalId());
                log.info("Changes commited");
                MDC.clear();
            }
        };
    }

    private Optional<Integer> sendTyping(long chatId) throws GatewayException {
        var chatAction = SendChatAction.builder()
                .action(ActionType.TYPING.toString())
                .chatId(chatId)
                .build();
        try {
            log.debug("Sending typing action");
            telegramClient.execute(chatAction);
            return Optional.empty();
        } catch (TelegramApiException ex) {
            log.error("Sending typing action error");
            throw new GatewayException(ex);
        }
    }

    private Optional<Integer> sendMessage(
            long chatId, String text, String parseMode, InlineKeyboardMarkup keyboardMarkup
    ) throws GatewayException {
        var sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(parseMode)
                .linkPreviewOptions(LinkPreviewOptions.builder()
                        .isDisabled(true)
                        .build()
                )
                .replyMarkup(keyboardMarkup);
        try {
            log.debug("Sending message");
            return Optional.of(telegramClient.execute(sendMessage.build()).getMessageId());
        } catch (Exception ex) {
            log.error("Sending message error");
            throw new GatewayException(ex);
        }
    }

    private Optional<Integer> editMessage(
            long chatId, int messageId, String text, String parseMode, InlineKeyboardMarkup replyMarkup
    ) throws GatewayException {
        if (text != null) {
            editText(chatId, messageId, text, parseMode);
        }
        editReplyMarkup(chatId, messageId, replyMarkup);
        return Optional.empty();
    }

    private void editText(long chatId, int messageId, String text, String parseMode) throws GatewayException {
        var editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .parseMode(parseMode)
                .linkPreviewOptions(LinkPreviewOptions.builder()
                        .isDisabled(true)
                        .build()
                )
                .build();
        try {
            log.debug("Editing message text");
            telegramClient.execute(editMessage);
        } catch (Exception ex) {
            log.error("Editing message text error");
            throw new GatewayException(ex);
        }
    }

    private void editReplyMarkup(long chatId, int messageId, InlineKeyboardMarkup replyMarkup) throws GatewayException {
        var editReplyMarkup = EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(replyMarkup)
                .build();
        try {
            log.debug("Editing reply markup");
            telegramClient.execute(editReplyMarkup);
        } catch (Exception ex) {
            log.error("Editing reply markup error");
            throw new GatewayException(ex);
        }
    }

    private Optional<Integer> deleteMessage(long chatId, int messageId) throws GatewayException {
        var deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId);
        try {
            log.debug("Deleting message");
            telegramClient.execute(deleteMessage.build());
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Deleting message error");
            throw new GatewayException(ex);
        }
    }

    private Optional<InlineKeyboardMarkup> getInlineKeyboardMarkup(OutputData outputData) {
        var buttons = outputData.buttons().stream().map(row ->
                new InlineKeyboardRow(row.stream().map(button ->
                        InlineKeyboardButton.builder()
                                .text(button.display())
                                .callbackData(button.data())
                                .build()
                ).toList())
        ).toList();
        if (!buttons.isEmpty()) {
            var markup = InlineKeyboardMarkup.builder()
                    .keyboard(buttons)
                    .build();
            return Optional.of(markup);
        }
        return Optional.empty();
    }
}

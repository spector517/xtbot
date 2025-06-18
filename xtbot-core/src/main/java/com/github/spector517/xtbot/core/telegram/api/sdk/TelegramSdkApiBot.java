package com.github.spector517.xtbot.core.telegram.api.sdk;

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
import com.github.spector517.xtbot.core.mapper.TgSdkUpdateToDataMapper;
import com.github.spector517.xtbot.core.properties.Properties;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.core.telegram.api.token.BotAuthLoader;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
public class TelegramSdkApiBot extends TelegramLongPollingBot implements Gateway {

    private final BotAuthLoader botAuthLoader;
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

    private final Map<Long, Future<UpdateData>> inProgressEvents;
    private final Config config;

    @Data
    @Accessors(fluent = true, chain = true)
    public static class Parameters {
        private BotAuthLoader botAuthLoader;
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
    }

    @SneakyThrows
    public TelegramSdkApiBot(Parameters params) {
        super(params.botAuthLoader.getToken());
        this.botAuthLoader = params.botAuthLoader;
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

        this.inProgressEvents = new HashMap<>();
        this.config = new Config(this);
    }

    @Override
    public synchronized void onUpdateReceived(org.telegram.telegrambots.meta.api.objects.Update update) {
        try {
            var clientId = TgSdkUpdateToDataMapper.getClientId(update);
            MDC.put(ClientData.EXTERNAL_ID_KEY, String.valueOf(clientId));
            if (inProgressEvents.containsKey(clientId)) {
                log.warn("Client has uncompleted events. Skipping.");
                return;
            }

            var updateData = sdkMapper.map(update);
            var handler = consume(updateData, config);
            var wrappedHandler = wrapHandler(handler, updateData);
            log.info("Submitting event");
            inProgressEvents.put(clientId, executorService.submit(wrappedHandler));

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
    public String getBotUsername() {
        return botAuthLoader.getUsername();
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
        return commonMethodsLoader;
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

    private Callable<UpdateData> wrapHandler(Callable<UpdateData> handler, UpdateData updateData) {
        var mdsContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                MDC.setContextMap(mdsContext);
                log.info("Starting event processing");
                var data = handler.call();
                var entity = toEntityMapper.map(data.client());
                clientRepository.save(entity);
                log.info("Event processing completed successfully");
                return data;
            } catch (Exception ex) {
                var entity = toEntityMapper.map(updateData.client());
                clientRepository.save(entity);
                log.error("Event processing failed: {}", ex.getMessage());
                log.debug("Stack trace:", ex);
                throw ex;
            } finally {
                inProgressEvents.remove(updateData.client().externalId());
                MDC.clear();
            }
        };
    }

    private void sendTyping(OutputData outputData) throws GatewayException {
        var chatAction = SendChatAction.builder()
                .action(ActionType.TYPING.toString())
                .chatId(outputData.chatId())
                .build();
        try {
            log.debug("Sending typing action");
            execute(chatAction);
        } catch (TelegramApiException ex) {
            log.error("Sending typing action error");
            throw new GatewayException(ex);
        }
    }

    private void removeButtons(OutputData outputData) throws GatewayException {
        if (outputData.removeButtons()) {
            try {
                log.debug("Removing buttons");
                execute(EditMessageReplyMarkup.builder()
                        .chatId(outputData.chatId())
                        .messageId(outputData.previousSendedMessageId())
                        .build()
                );
            } catch (TelegramApiException ex) {
                log.error("Removing buttons error");
                throw new GatewayException(ex);
            }
        }
    }

    private int sendMessage(OutputData outputData) throws GatewayException {
        var sendMessage = SendMessage.builder()
                .text(outputData.text())
                .parseMode(outputData.parseMode())
                .chatId(outputData.chatId());
        getInlineKeyboardMarkup(outputData).ifPresent(sendMessage::replyMarkup);
        try {
            log.debug("Sending message");
            return execute(sendMessage.build()).getMessageId();
        } catch (Exception ex) {
            log.error("Sending message error");
            throw new GatewayException(ex);
        }
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

    private int editTextMessage(OutputData outputData) throws GatewayException {
        var editMessage = EditMessageText.builder()
                .text(outputData.text())
                .parseMode(outputData.parseMode())
                .chatId(outputData.chatId())
                .messageId(outputData.messageId());
        getInlineKeyboardMarkup(outputData).ifPresent(editMessage::replyMarkup);
        try {
            log.debug("Editing message");
            execute(editMessage.build());
            return outputData.messageId();
        } catch (Exception ex) {
            log.error("Editing message error");
            throw new GatewayException(ex);
        }
    }

    private int editButtons(OutputData outputData) throws GatewayException {
        var editButtons = EditMessageReplyMarkup.builder()
                .chatId(outputData.chatId())
                .messageId(outputData.messageId());
        getInlineKeyboardMarkup(outputData).ifPresent(editButtons::replyMarkup);
        try {
            log.debug("Editing buttons");
            execute(editButtons.build());
            return outputData.messageId();
        } catch (Exception ex) {
            log.error("Editing buttons error");
            throw new GatewayException(ex);
        }
    }

    private void deleteMessage(OutputData outputData) throws GatewayException {
        var deleteMessage = DeleteMessage.builder()
                .chatId(outputData.chatId())
                .messageId(outputData.deleteMessageId());
        try {
            log.debug("Deleting message");
            execute(deleteMessage.build());
        } catch (Exception ex) {
            log.error("Deleting message error");
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
}

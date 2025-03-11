package com.github.spector517.xtbot.core.mapper;

import com.github.spector517.xtbot.core.application.data.inbound.*;
import com.github.spector517.xtbot.core.application.logger.MDCLogManager;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.mapper.MappingException;
import com.github.spector517.xtbot.core.application.repository.ClientEntity;
import com.github.spector517.xtbot.core.application.repository.ClientNotFoundException;
import com.github.spector517.xtbot.core.application.repository.ClientRepository;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Slf4j
public class TgSdkUpdateToDataMapper implements Mapper<Update, UpdateData> {

    private final ClientRepository clientRepository;
    private final Mapper<ClientEntity, ClientData> mapper;

    @Setter
    private String initialStageName;

    public static long getClientId(Update update) throws MappingException {
        var clientId =  switch (getUpdateType(update)) {
            case MESSAGE -> update.getMessage().getFrom().getId();
            case CALLBACK -> update.getCallbackQuery().getFrom().getId();
        };
        log.debug("Client Telegram ID: {}", clientId);
        return clientId;
    }

    @Override
    public UpdateData map(Update update) throws MappingException {
        if (initialStageName == null) {
            var msg = "Initial stage name is not set";
            log.debug(msg);
            throw new MappingException(msg);
        }
        var updateType = getUpdateType(update);
        var clientData = getClientData(update, updateType);
        var chatId = getChatId(update, updateType);
        var updateData = new UpdateData()
                .client(clientData)
                .chatId(chatId)
                .type(updateType);
        switch (updateType) {
            case MESSAGE -> updateData.message(
                    new MessageData()
                            .id(update.getMessage().getMessageId())
                            .text(update.getMessage().getText())
            );
            case CALLBACK -> updateData.callback(
                    new CallbackData()
                            .data(update.getCallbackQuery().getData())
            );
        }
        return updateData;
    }

    private static Type getUpdateType(Update update) throws MappingException {
        if (update.hasMessage()) {
            log.debug("Update type: {}", Type.MESSAGE);
            return Type.MESSAGE;
        }
        if (update.hasCallbackQuery()) {
            log.debug("Update type: {}", Type.CALLBACK);
            return Type.CALLBACK;
        }
        throw new MappingException("Unknown update type");
    }

    private ClientData getClientData(Update update, Type type) throws MappingException {
        var user = switch (type) {
            case MESSAGE -> update.getMessage().getFrom();
            case CALLBACK -> update.getCallbackQuery().getFrom();
        };
        var clientId = getClientId(update);
        MDCLogManager.putClientId(clientId);
        ClientData clientData;
        try {
            clientData = mapper.map(clientRepository.findByExternalId(clientId));
            MDCLogManager.put(clientData);
        } catch (ClientNotFoundException ex) {
            log.debug("Creating new client data");
            clientData = createClientData(clientId, user.getUserName());
            MDCLogManager.put(clientData);
            log.info("Created new client");
        } catch (MappingException ex) {
            throw ex;
        }
        return clientData;
    }

    private ClientData createClientData(long clientId, String userName) {
        return new ClientData()
                .externalId(clientId)
                .name(userName)
                .currentStage(initialStageName)
                .previousStages(List.of())
                .currentStageInitiated(true)
                .currentStageCompleted(false)
                .additionalVars(Map.of())
                .stageVars(Map.of());
    }

    private long getChatId(Update update, Type type) {
        return switch (type) {
            case MESSAGE -> update.getMessage().getChatId();
            case CALLBACK -> update.getCallbackQuery().getMessage().getChatId();
        };
    }
}

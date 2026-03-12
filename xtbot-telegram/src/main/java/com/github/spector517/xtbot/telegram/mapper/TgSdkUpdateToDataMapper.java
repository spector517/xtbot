package com.github.spector517.xtbot.telegram.mapper;

import com.github.spector517.xtbot.core.application.data.inbound.*;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.mapper.MappingException;
import com.github.spector517.xtbot.core.repository.ClientNotFoundException;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class TgSdkUpdateToDataMapper implements Mapper<UpdateData, Update> {

    public static final String COMMAND_REGEX = "^/(\\w+)\\s*(.*)";

    private final ClientRepository clientRepository;
    private final Mapper<ClientData, ClientEntity> mapper;
    private final String initialStageName;

    public static long getClientId(Update update) throws MappingException {
        var clientId =  switch (getUpdateType(update)) {
            case MESSAGE, COMMAND -> update.getMessage().getFrom().getId();
            case CALLBACK -> update.getCallbackQuery().getFrom().getId();
        };
        log.debug("Client Telegram ID: {}", clientId);
        return clientId;
    }

    @Override
    public UpdateData map(Update update, Object... ignored) throws MappingException {
        if (initialStageName == null) {
            throw new MappingException("Initial stage name is not set");
        }
        var updateType = getUpdateType(update);
        var clientData = getClientData(update, updateType);
        var chatId = getChatId(update, updateType);
        var updateData = new UpdateData()
                .client(clientData)
                .chatId(chatId)
                .type(updateType);
        switch (updateType) {
            case Type.MESSAGE -> {
                var msg = update.getMessage();
                var sentAt = msg.getDate() != null
                        ? LocalDateTime.ofEpochSecond(msg.getDate(), 0, ZoneOffset.UTC)
                        : null;
                updateData.message(new ChatMessage(msg.getMessageId(), msg.getText(), sentAt, MessageType.USER));
            }
            case Type.COMMAND -> updateData.command(getCommandData(update));
            case CALLBACK -> updateData.callback(
                    new CallbackData()
                            .data(update.getCallbackQuery().getData())
            );
        }
        return updateData;
    }

    private static Type getUpdateType(Update update) throws MappingException {
        Type updateType = null;
        if (update.hasMessage()) {
            updateType = update.getMessage().getText().matches(COMMAND_REGEX)
                    ? Type.COMMAND
                    : Type.MESSAGE;
        }
        if (update.hasCallbackQuery()) {
            updateType = Type.CALLBACK;
        }
        if (updateType == null) {
            throw new MappingException("Unknown update type");
        }
        log.debug("Update type: {}", updateType);
        return updateType;
    }

    private ClientData getClientData(Update update, Type type) throws MappingException {
        var user = switch (type) {
            case MESSAGE, COMMAND -> update.getMessage().getFrom();
            case CALLBACK -> update.getCallbackQuery().getFrom();
        };
        var clientId = getClientId(update);
        ClientData clientData;
        try {
            clientData = mapper.map(clientRepository.findByExternalId(clientId));
        } catch (ClientNotFoundException ex) {
            log.debug("Creating new client data");
            clientData = createClientData(clientId, user.getUserName());
            log.debug("Created new client");
        }
        return clientData;
    }

    private ClientData createClientData(long clientId, String userName) {
        return new ClientData()
                .externalId(clientId)
                .name(userName)
                .bindNewStage(initialStageName)
                .setStageInitiated()
                .additionalVars(Map.of())
                .stageVars(Map.of());
    }

    private long getChatId(Update update, Type type) {
        return switch (type) {
            case MESSAGE, COMMAND -> update.getMessage().getChatId();
            case CALLBACK -> update.getCallbackQuery().getMessage().getChatId();
        };
    }

    private CommandData getCommandData(Update update) {
        var messageId = update.getMessage().getMessageId();
        var matcher = Pattern.compile(COMMAND_REGEX).matcher(update.getMessage().getText());
        if (!matcher.find()) {
            throw new IllegalStateException("Command not found");
        }
        var name = matcher.group(1);
        List<String> args = matcher.group(2).isBlank()
                ? List.of()
                : Arrays.stream(matcher.group(2).split("\\s+")).toList();
        return new CommandData()
                .messageId(messageId)
                .name(name)
                .args(args);
    }
}

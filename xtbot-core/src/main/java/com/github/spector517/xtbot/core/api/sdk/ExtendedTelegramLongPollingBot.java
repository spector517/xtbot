package com.github.spector517.xtbot.core.api.sdk;

import java.util.Optional;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.gateway.GatewayException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class ExtendedTelegramLongPollingBot extends TelegramLongPollingBot implements Gateway<Update> {

    protected ExtendedTelegramLongPollingBot(String token) {
        super(token);
    }

    protected void sendTyping(OutputData outputData) throws GatewayException {
        var chatAction = SendChatAction.builder()
            .action(ActionType.TYPING.toString())
            .chatId(outputData.chatId())
            .build();
        try {
            execute(chatAction);
        } catch (TelegramApiException ex) {
            log.error("Sending typing action error");
            throw new GatewayException(ex);
        }
    }

    protected void removeButtons(OutputData outputData) throws GatewayException {
        if (outputData.removeButtons()) {
            try {
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

    protected int sendMessage(OutputData outputData) throws GatewayException {
        var sendMessage = SendMessage.builder()
            .text(outputData.text())
            .parseMode(outputData.parseMode())
            .chatId(outputData.chatId());
        getInlineKeyboardMarkup(outputData).ifPresent(sendMessage::replyMarkup);
        try {
            return execute(sendMessage.build()).getMessageId();
        } catch (Exception ex) {
            log.error("Sending message error");
            throw new GatewayException(ex);
        }
    }

    protected int editTextMessage(OutputData outputData) throws GatewayException {
        var editMessage = EditMessageText.builder()
            .text(outputData.text())
            .parseMode(outputData.parseMode())
            .chatId(outputData.chatId())
            .messageId(outputData.messageId());
        getInlineKeyboardMarkup(outputData).ifPresent(editMessage::replyMarkup);
        try {
            execute(editMessage.build());
            return outputData.messageId();
        } catch (Exception ex) {
            log.error("Editing message error");
            throw new GatewayException(ex);
        }
    }

    protected int editButtons(OutputData outputData) throws GatewayException {
        var editButtons = EditMessageReplyMarkup.builder()
            .chatId(outputData.chatId())
            .messageId(outputData.messageId());
        getInlineKeyboardMarkup(outputData).ifPresent(editButtons::replyMarkup);
        try {
            execute(editButtons.build());
            return outputData.messageId();
        } catch (Exception ex) {
            log.error("Editing buttons error");
            throw new GatewayException(ex);
        }
    }

    protected void deleteMessage(OutputData outputData) throws GatewayException {
        var deleteMessage = DeleteMessage.builder()
            .chatId(outputData.chatId())
            .messageId(outputData.deleteMessageId());
        try {
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

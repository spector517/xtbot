package com.github.spector517.xtbot.core.mapper;

import com.github.spector517.xtbot.api.dto.*;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;

public class UpdateDataToBotApiMapper implements Mapper<Update, UpdateData> {

    @Override
    public Update map(UpdateData updateData, Object... ignored) {
        var clientData = updateData.client();
        var client = new Client(
                clientData.externalId(),
                clientData.name(),
                clientData.stageName(),
                clientData.stageInitiated(),
                clientData.stageCompleted(),
                clientData.previousStages(),
                clientData.additionalVars(),
                clientData.stageVars()
        );
        var messageData = updateData.message();
        var message = messageData != null ? new Message(messageData.telegramMessageId(), messageData.text()) : null;
        var callbackData = updateData.callback();
        var callback = callbackData != null ? new Callback(callbackData.data()) : null;
        var commandData = updateData.command();
        var command = commandData != null
                ? new Command(commandData.messageId(), commandData.name(), commandData.args())
                : null;
        return new Update(client, updateData.chatId(), message, callback, command);
    }
}

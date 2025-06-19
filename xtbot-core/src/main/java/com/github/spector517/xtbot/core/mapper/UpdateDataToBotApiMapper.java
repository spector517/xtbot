package com.github.spector517.xtbot.core.mapper;

import com.github.spector517.xtbot.api.dto.Callback;
import com.github.spector517.xtbot.api.dto.Client;
import com.github.spector517.xtbot.api.dto.Message;
import com.github.spector517.xtbot.api.dto.Update;
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
        var message = messageData != null ? new Message(messageData.id(), messageData.text()) : null;
        var callbackData = updateData.callback();
        var callback = callbackData != null ? new Callback(callbackData.data()) : null;
        return new Update(client, updateData.chatId(), message, callback);
    }
}

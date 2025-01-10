package com.github.spector517.xtbot.core.mapper;

import com.github.spector517.xtbot.api.dto.Callback;
import com.github.spector517.xtbot.api.dto.Client;
import com.github.spector517.xtbot.api.dto.Message;
import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.mapper.MappingException;

public class UpdateDataToBotApiMapper implements Mapper<UpdateData, Update> {

    @Override
    public Update map(UpdateData updateData) throws MappingException {
        var clientData = updateData.client();
        var client = new Client(
                clientData.externalId(),
                clientData.name(),
                clientData.currentStage(),
                clientData.currentStageInitiated(),
                clientData.currentStageCompleted(),
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

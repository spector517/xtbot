package com.github.spector517.xtbot.core.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateContext {

    private final MessageContext message;
    private final CallbackContext callback;

    public UpdateContext(UpdateData updateData) {
        this.message = updateData.message() != null ? new MessageContext(updateData.message()) : null;
        this.callback = updateData.callback() != null ? new CallbackContext(updateData.callback()) : null;
    }
}

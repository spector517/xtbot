package com.github.spector517.xtbot.core.application.data.inbound;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class UpdateData {

    private ClientData client;
    private long chatId;
    private Type type;
    private MessageData message;
    private CallbackData callback;
}

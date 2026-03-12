package com.github.spector517.xtbot.core.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.spector517.xtbot.core.application.data.inbound.ChatMessage;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageContext {

    private final int id;
    private final String text;

    public MessageContext(ChatMessage chatMessage) {
        this.id = chatMessage.telegramMessageId() != null ? chatMessage.telegramMessageId() : 0;
        this.text = chatMessage.text();
    }
}

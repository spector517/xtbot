package com.github.spector517.xtbot.core.application.data.inbound;

import java.time.LocalDateTime;

public record ChatMessage(
        Integer telegramMessageId,
        String text,
        LocalDateTime sentAt,
        MessageType type
) {}

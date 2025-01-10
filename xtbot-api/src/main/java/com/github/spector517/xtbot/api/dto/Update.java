package com.github.spector517.xtbot.api.dto;

public record Update(
        Client client,
        long chatId,
        Message message,
        Callback callback
) {}

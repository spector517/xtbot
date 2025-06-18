package com.github.spector517.xtbot.core.telegram.api.auth;

public interface BotAuthLoader {

    @SuppressWarnings("SameReturnValue")
    default String getUsername() {
        return "XTBot";
    }

    String getToken();
}

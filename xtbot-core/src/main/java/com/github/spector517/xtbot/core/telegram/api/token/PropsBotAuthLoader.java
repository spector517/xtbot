package com.github.spector517.xtbot.core.telegram.api.token;

import com.github.spector517.xtbot.core.properties.Properties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class PropsBotAuthLoader implements BotAuthLoader {

    private final Properties properties;

    @Override
    public String getUsername() {
        return "XTBot";
    }

    @Override
    @SneakyThrows
    public String getToken() {
        return properties.botToken();
    }
}

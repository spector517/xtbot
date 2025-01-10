package com.github.spector517.xtbot.core.api.sdk;

import org.telegram.telegrambots.meta.api.objects.Update;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.handler.EventHandler;

public class TelegramSdkEventHandler extends EventHandler<Update> {

    TelegramSdkEventHandler(
        Update update, 
        Config config,
        ComponentsContainer container,
        Gateway<Update> gateway
    ) {
        super(update, config, container, gateway);
    }
}

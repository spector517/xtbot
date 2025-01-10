package com.github.spector517.xtbot.core.application.handler;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.gateway.Gateway;

public class TestEventHandler extends EventHandler<Object> {

    TestEventHandler(
        Object update,
        Config config,
        ComponentsContainer componentsContainer,
        Gateway<Object> gateway
    ) {
        super(update, config, componentsContainer, gateway);
    }
}

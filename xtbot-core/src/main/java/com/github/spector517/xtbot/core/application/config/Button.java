package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.properties.ButtonProps;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class Button {

    private final Template display;
    private final Template data;

    Button(ButtonProps props, Gateway gateway) {
        this.display = new Template(gateway.getRender(), props.display());
        this.data = new Template(gateway.getRender(), props.data());
    }
}

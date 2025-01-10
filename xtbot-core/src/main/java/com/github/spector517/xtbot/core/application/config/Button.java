package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.properties.ButtonProps;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class Button {

    private final Template display;
    private final Template data;

    Button(ButtonProps props, ComponentsContainer componentsContainer) {
        this.display = new Template(componentsContainer.render(), props.display());
        this.data = new Template(componentsContainer.render(), props.data());
    }
}

package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.properties.MessageProps;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;

@Getter
@Accessors(fluent = true)
public class Message {

    private final Optional<Template> text;
    private final ParseMode parseMode;
    private final List<List<Button>> buttons;

    Message(MessageProps props, ComponentsContainer container) {
        this.text = props.text() == null || props.text().isBlank()
            ? Optional.empty()
            : Optional.of(new Template(container.render(), props.text()));
        this.parseMode = switch (props.parseMode()) {
            case MARKDOWN -> ParseMode.MARKDOWN;
            case PLAIN_TEXT -> ParseMode.PLAIN_TEXT;
            case null -> ParseMode.PLAIN_TEXT;
        };
        if (props.buttons() == null) {
            this.buttons = List.of();
            return;
        }
        this.buttons = props.buttons().stream().map(rowButtonsProps ->
                rowButtonsProps.stream()
                        .filter(buttonProps -> 
                            buttonProps.display() != null && buttonProps.data() != null
                        )
                        .filter(buttonProps -> 
                            !buttonProps.display().isBlank() && !buttonProps.data().isBlank()
                        )
                        .map(buttonProps -> new Button(buttonProps, container))
                        .toList()
        ).toList();
    }
}

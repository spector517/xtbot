package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.properties.data.MessageProps;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;

@Getter
@Accessors(fluent = true)
public class Message {

    private final Template id;
    private final Template deleteId;
    private final Template text;
    private final ParseMode parseMode;
    private final List<List<Button>> buttons;
    private final Gateway gateway;

    Message(MessageProps props, Gateway gateway) {
        this.id = props.id() == null || props.id().isBlank()
                ? null
                : new Template(gateway.getRender(), props.id());
        this.deleteId = props.delete() == null || props.delete().isBlank()
                ? null
                : new Template(gateway.getRender(), props.delete());
        this.text = props.text() == null || props.text().isBlank()
                ? null
                : new Template(gateway.getRender(), props.text());
        this.parseMode = switch (props.parseMode()) {
            case MARKDOWN -> ParseMode.MARKDOWN;
            case MARKDOWN_V2 -> ParseMode.MARKDOWN_V2;
            case PLAIN_TEXT -> ParseMode.PLAIN_TEXT;
            case null -> ParseMode.PLAIN_TEXT;
        };
        if (props.buttons() == null) {
            this.buttons = List.of();
        } else {
            this.buttons = props.buttons().stream().map(rowButtonsProps ->
                    rowButtonsProps.stream()
                            .filter(buttonProps ->
                                    buttonProps.display() != null && buttonProps.data() != null
                            )
                            .filter(buttonProps ->
                                    !buttonProps.display().isBlank() && !buttonProps.data().isBlank()
                            )
                            .map(buttonProps -> new Button(buttonProps, gateway))
                            .toList()
            ).toList();
        }
        this.gateway = gateway;
    }

    public Optional<Template> id() {
        return Optional.ofNullable(id);
    }

    public Optional<Template> deleteId() {
        return Optional.ofNullable(deleteId);
    }

    public Optional<Template> text() {
        return Optional.ofNullable(text);
    }
}

package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.utils.CommonUtils;
import com.github.spector517.xtbot.core.properties.AcceptorProps;
import com.github.spector517.xtbot.core.properties.StageProps;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@Accessors(fluent = true)
@SuppressWarnings("unchecked")
public class Stage {

    public static final String CALLBACK_ACCEPTOR_NAME = "xtbot.internal.callback";

    private final String name;
    private final boolean initial;
    private final boolean fail;
    private final Optional<Message> message;
    private final boolean removeButtons;
    private final boolean autocomplete;
    private final List<Acceptor> acceptors;
    private final List<Action> actions;
    private final Map<String, Object> save;
    private final Optional<Template> next;

    Stage(StageProps props, ComponentsContainer container) {
        this.name = props.name();
        this.initial = props.initial() != null && props.initial();
        this.fail = props.fail() != null && props.fail();
        this.message = props.message() == null 
            ? Optional.empty()
            : Optional.of(new Message(props.message(), container));
        this.removeButtons = props.removeButtons() == null || props.removeButtons();
        this.autocomplete = props.autocomplete() != null && props.autocomplete();

        List<Acceptor> definedAcceptors;
        if (props.accept() != null) {
            definedAcceptors = props.accept().stream().map(acceptorProps ->
                    new Acceptor(acceptorProps, container)
            ).toList();
        } else {
            definedAcceptors = List.of();
        }

        if (props.actions() != null) {
            this.actions = props.actions().stream().map(actionProps ->
                    new Action(actionProps, container)
            ).toList();
        } else {
            this.actions = List.of();
        }

        if (props.save() != null) {
            this.save = (Map<String, Object>) CommonUtils.getTemplatedMap(props.save(), container.render());
        } else {
            this.save = Map.of();
        }

        this.next = props.next() == null || props.next().isBlank() 
            ? Optional.empty()
            : Optional.of(new Template(container.render(), props.next()));

        var additionalAcceptors = new ArrayList<Acceptor>();
        message.ifPresent(mess ->
            mess.buttons().stream().flatMap(Collection::stream).forEach(button ->
                additionalAcceptors.add(new Acceptor(
                    new AcceptorProps(CALLBACK_ACCEPTOR_NAME, button.data().rawValue()), 
                    container
                ))
            )
        );
        this.acceptors = this.autocomplete 
            ? List.of() 
            : Stream.concat(definedAcceptors.stream(), additionalAcceptors.stream()).toList();
    }

    public Map<String, Object> getAdditionalVars(Map<String, Object> context) {
        return (Map<String, Object>) CommonUtils.getFilledMap(save, context);
    }
}

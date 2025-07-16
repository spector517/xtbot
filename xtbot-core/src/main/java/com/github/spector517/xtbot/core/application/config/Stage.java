package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.utils.CommonUtils;
import com.github.spector517.xtbot.core.properties.data.AcceptorProps;
import com.github.spector517.xtbot.core.properties.data.StageProps;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.stream.Stream;

@Getter
@Accessors(fluent = true)
@SuppressWarnings("unchecked")
public class Stage {

    public static final String CALLBACK_ACCEPTOR_NAME = "xtbot.internal.callback";

    private final String name;
    private final boolean initial;
    private final boolean fail;
    private final Message message;
    private final boolean removeButtons;
    private final boolean autocomplete;
    private final List<Acceptor> acceptors;
    private final List<Action> actions;
    private final Map<String, Object> save;
    private final Template next;

    Stage(
            StageProps props,
            Gateway gateway,
            AcceptorChecker acceptorChecker,
            AcceptorLoader acceptorLoader,
            ExecutorChecker executorChecker,
            ExecutorLoader executorLoader
    ) {
        this.name = props.name();
        this.initial = props.initial() != null && props.initial();
        this.fail = props.fail() != null && props.fail();
        this.message = props.message() == null 
            ? null
            : new Message(props.message(), gateway);
        this.removeButtons = props.removeButtons() == null || props.removeButtons();
        this.autocomplete = props.autocomplete() != null && props.autocomplete();

        List<Acceptor> definedAcceptors;
        if (props.accept() != null) {
            definedAcceptors = props.accept().stream().map(acceptorProps ->
                    new Acceptor(acceptorProps, gateway, acceptorChecker, acceptorLoader)
            ).toList();
        } else {
            definedAcceptors = List.of();
        }

        if (props.actions() != null) {
            this.actions = props.actions().stream().map(actionProps ->
                    new Action(actionProps, gateway, executorChecker, executorLoader)
            ).toList();
        } else {
            this.actions = List.of();
        }

        if (props.save() != null) {
            this.save = (Map<String, Object>) CommonUtils.getTemplatedMap(props.save(), gateway.getRender());
        } else {
            this.save = Map.of();
        }

        this.next = props.next() == null || props.next().isBlank() 
            ? null
            : new Template(gateway.getRender(), props.next());

        var additionalAcceptors = new ArrayList<Acceptor>();
        if (message != null) {
            message.buttons().stream().flatMap(Collection::stream).forEach(button ->
                additionalAcceptors.add(new Acceptor(
                    new AcceptorProps(CALLBACK_ACCEPTOR_NAME, button.data().rawValue()),
                    gateway,
                    acceptorChecker,
                    acceptorLoader
                ))
            );
        }
        this.acceptors = this.autocomplete
            ? List.of() 
            : Stream.concat(definedAcceptors.stream(), additionalAcceptors.stream()).toList();
    }

    public Optional<Message> message() {
        return Optional.ofNullable(message);
    }

    public Optional<Template> next() {
        return Optional.ofNullable(next);
    }

    public Map<String, Object> getAdditionalVars(Map<String, Object> context) {
        return (Map<String, Object>) CommonUtils.getFilledMap(save, context);
    }
}

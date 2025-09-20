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

    public static final String CALLBACK_ACCEPTOR_NAME = "x.accept.callback";

    private final String name;
    private final boolean initial;
    private final boolean fail;
    private final Message message;
    private final boolean removeButtons;
    private final boolean autocomplete;
    private final boolean sendTyping;
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
        this.sendTyping = props.sendTyping() == null || props.sendTyping();


        List<Acceptor> definedAcceptors = props.accept() != null
                ? props.accept().stream().map(acceptorProps ->
                        new Acceptor(acceptorProps, gateway, acceptorChecker, acceptorLoader)
                    ).toList()
                : List.of();

        this.actions = props.actions() != null
            ? props.actions().stream().map(actionProps ->
                    new Action(actionProps, gateway, executorChecker, executorLoader)
                ).toList()
            : List.of();

        this.save = props.save() != null
                ? (Map<String, Object>) CommonUtils.getTemplatedMap(props.save(), gateway.getRender())
                : Map.of();

        this.next = props.next() == null || props.next().isBlank() 
            ? null
            : new Template(gateway.getRender(), props.next());


        List<Acceptor> additionalAcceptors = message != null
                ? message.buttons().stream().flatMap(Collection::stream).map(button ->
                        new Acceptor(
                                new AcceptorProps(CALLBACK_ACCEPTOR_NAME, button.data().rawValue()),
                                gateway,
                                acceptorChecker,
                                acceptorLoader
                        )
                    ).toList()
                : List.of();

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

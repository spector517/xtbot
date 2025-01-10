package com.github.spector517.xtbot.core.application.extension.acceptor;

import com.github.spector517.xtbot.api.dto.Update;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class AcceptorCheckerV1 implements AcceptorChecker {

    @Override
    public void checkAcceptor(Method method) throws AcceptorCheckFailedException {
        var parameters = method.getParameters();
        if (
                parameters.length != 2
                || !parameters[0].getType().equals(Update.class)
                || !parameters[1].getType().equals(String.class)
        ) {
            throw new AcceptorCheckFailedException(getErrorMessage(parameters));
        }
        if (!method.getReturnType().isAssignableFrom(boolean.class)) {
            throw new AcceptorCheckFailedException(
                "Return type of Acceptor method '%s' must be a boolean, not [%s]".formatted(
                    method.getName(),
                    method.getReturnType()
                )
            );
        }
    }

    private String getErrorMessage(Parameter[] parameters) {
        var wrappedParametersTypes = Arrays.stream(parameters).map(parameter ->
                "[%s]".formatted(parameter.getType())
        ).toList();
        return "Acceptor method must have 2 parameters: [%s], [%s], but got %d: %s".formatted(
                Update.class, String.class, parameters.length, 
                String.join(", ", wrappedParametersTypes)
        );
    }
}

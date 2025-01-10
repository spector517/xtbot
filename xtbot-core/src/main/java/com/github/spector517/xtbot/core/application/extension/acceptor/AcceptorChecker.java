package com.github.spector517.xtbot.core.application.extension.acceptor;

import java.lang.reflect.Method;

public interface AcceptorChecker {

    static AcceptorChecker getAcceptorChecker(int version) {
        if (version == 1) {
            return new AcceptorCheckerV1();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported version of AcceptorChecker: ".concat(String.valueOf(version))
            );
        }
    }

    void checkAcceptor(Method method) throws AcceptorCheckFailedException;
}
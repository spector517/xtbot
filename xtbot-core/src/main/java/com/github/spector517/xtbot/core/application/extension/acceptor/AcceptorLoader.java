package com.github.spector517.xtbot.core.application.extension.acceptor;

import java.lang.reflect.Method;

public interface AcceptorLoader {

    Method getAcceptor(String name) throws AcceptorNotFoundException;
}

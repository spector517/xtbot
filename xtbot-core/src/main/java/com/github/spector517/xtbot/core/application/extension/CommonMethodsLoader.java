package com.github.spector517.xtbot.core.application.extension;

import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorNotFoundException;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorNotFoundException;
import com.github.spector517.xtbot.core.application.loader.BotClassLoader;

import java.lang.reflect.Method;
import java.util.Arrays;

public class CommonMethodsLoader implements AcceptorLoader, ExecutorLoader {

    private final BotClassLoader[] botClassLoaders;

    public CommonMethodsLoader(BotClassLoader... botClassLoaders) {
        this.botClassLoaders = botClassLoaders;
    }

    @Override
    public Method getAcceptor(String name) throws AcceptorNotFoundException {
        return Arrays.stream(botClassLoaders).flatMap(loader ->
                loader.getBotComponents().stream().flatMap(component ->
                        Arrays.stream(component.getMethods()).filter(method ->
                                method.isAnnotationPresent(Acceptor.class)
                                    && method.getAnnotation(Acceptor.class).value()
                                        .equals(name)
                        )
                )
        ).findAny().orElseThrow(() ->
                new AcceptorNotFoundException("Acceptor '%s' not found".formatted(name))
        );
    }

    @Override
    public Method getExecutor(String name) throws ExecutorNotFoundException {
        return Arrays.stream(botClassLoaders).flatMap(loader ->
                loader.getBotComponents().stream().flatMap(component ->
                        Arrays.stream(component.getMethods()).filter(method ->
                                method.isAnnotationPresent(Executor.class)
                                        && method.getAnnotation(Executor.class).value()
                                                .equals(name)
                        )
                )
        ).findAny().orElseThrow(() ->
                new ExecutorNotFoundException("Executor '%s' not found".formatted(name))
        );
    }
}

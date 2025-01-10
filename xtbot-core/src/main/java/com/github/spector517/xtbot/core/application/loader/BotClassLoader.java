package com.github.spector517.xtbot.core.application.loader;

import java.util.Set;

public abstract class BotClassLoader extends ClassLoader {

    public abstract Set<Class<?>> getBotComponents();

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return getBotComponents().stream().filter(clazz ->
                clazz.getName().equals(name)
        ).findFirst().orElseThrow(() -> new ClassNotFoundException(name));
    }
}

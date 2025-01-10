package com.github.spector517.xtbot.core.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;

class InternalClassLoaderTest {

    @Test
    @SneakyThrows
    void getBotComponents() {
        var internalClassLoader = new InternalClassLoader();
        var expectedComponents = Set.of(
                getClass()
                        .getClassLoader()
                        .loadClass("com.github.spector517.xtbot.test.TestBotComponent")
        );
        assertEquals(expectedComponents, internalClassLoader.getBotComponents());
    }
}
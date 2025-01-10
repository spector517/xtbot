package com.github.spector517.xtbot.core.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;

class ExternalJarClassLoaderTest {

    private static String jarFileLocation;
    private static List<String> botClassNames;

    @BeforeAll
    @SneakyThrows
    static void setUp() {
        botClassNames = List.of("com.github.spector517.xtbot.test.TestBotComponent");
        var resourceName = "bot-test/bot-test.jar";
        try(var jarIs = ExternalJarClassLoader.class
                .getClassLoader().getResourceAsStream(resourceName)) {
            if (jarIs == null) {
                throw new IllegalStateException("Resource %s not found".formatted(resourceName));
            }
            var tempFile = Files.createTempFile("bot-test", ".jar");
            Files.copy(jarIs, tempFile, StandardCopyOption.REPLACE_EXISTING);
            jarFileLocation = tempFile.toString();
        }
    }

    @Test
    @DisplayName("Jar file location is null or blank")
    void testConstructor_0() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExternalJarClassLoader(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExternalJarClassLoader("")
        );
    }

    @Test
    @DisplayName("Jar file not found")
    void testConstructor_1() {
        assertThrows(
            FileNotFoundException.class,
            () -> new ExternalJarClassLoader("path/to/invalid/jar/file.jar")
        );
    }

    @Test
    @DisplayName("Jar file found and bot classes loaded")
    void testConstructor_2() {
        var botClassLoader = new ExternalJarClassLoader(jarFileLocation);
        var actualClassNames = botClassLoader.getBotComponents().stream().map(Class::getName).toList();
        assertEquals(botClassNames, actualClassNames);
    }
}

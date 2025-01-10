package com.github.spector517.xtbot.core.loader;

import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.core.application.loader.BotClassLoader;

import lombok.SneakyThrows;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class ExternalJarClassLoader extends BotClassLoader {

    private final String jarFileLocation;
    private final Set<Class<?>> botComponents;

    @SneakyThrows
    public ExternalJarClassLoader(String jarFileLocation) {
        if (jarFileLocation == null || jarFileLocation.isBlank()) {
            throw new IllegalArgumentException("External Jar file is not defined");
        }
        if (!Files.exists(Paths.get(jarFileLocation))) {
            throw new FileNotFoundException("External Jar file not found: %s".formatted(jarFileLocation));
        }
        this.jarFileLocation = jarFileLocation;
        this.botComponents = new HashSet<>();
        addBotClasses();
    }

    @Override
    public Set<Class<?>> getBotComponents() {
        return botComponents;
    }

    @SneakyThrows
    private void addBotClasses() {
        try (var jarFile = new JarFile(this.jarFileLocation)) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    var classBytes = jarFile.getInputStream(entry).readAllBytes();
                    var className = entry.getName()
                        .replace('/', '.')
                        .replace(".class", "");
                    var clazz = getParent().loadClass(className);
                    if (clazz == null) {
                        clazz = defineClass(className, classBytes, 0, classBytes.length);
                    }
                    if (clazz.isAnnotationPresent(BotComponent.class)) {
                        botComponents.add(clazz);
                    }
                }
            }
        }
    }
}

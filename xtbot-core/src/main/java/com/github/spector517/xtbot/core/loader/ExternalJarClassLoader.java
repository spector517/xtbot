package com.github.spector517.xtbot.core.loader;

import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.core.application.loader.BotClassLoader;
import com.github.spector517.xtbot.core.loader.asm.AnnotationDetector;
import lombok.SneakyThrows;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class ExternalJarClassLoader extends BotClassLoader {

    private final String jarFileLocation;
    private final Set<Class<?>> botComponents;
    private final ClassLoader urlClassLoader;

    @SneakyThrows
    public ExternalJarClassLoader(String jarFileLocation) {
        if (jarFileLocation == null || jarFileLocation.isBlank()) {
            throw new IllegalArgumentException("External Jar file is not defined");
        }
        var jarPath = Path.of(jarFileLocation);
        if (!Files.exists(jarPath)) {
            throw new FileNotFoundException("External Jar file not found: %s".formatted(jarFileLocation));
        }
        this.jarFileLocation = jarFileLocation;
        this.botComponents = new HashSet<>();
        this.urlClassLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getParent());
        addBotClasses();
    }

    @Override
    public Set<Class<?>> getBotComponents() {
        return botComponents;
    }

    @SneakyThrows
    private void addBotClasses() {
        try (var jarFile = new JarFile(jarFileLocation)) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    final byte[] classBytes;
                    try(var entryStream = jarFile.getInputStream(entry)) {
                        classBytes = entryStream.readAllBytes();
                    }
                    var className = entry.getName()
                        .replace('/', '.')
                        .replace(".class", "");
                    if (AnnotationDetector.isAnnotationPresentInClass(classBytes, BotComponent.class)) {
                        var clazz = urlClassLoader.loadClass(className);
                        botComponents.add(clazz);
                    }
                }
            }
        }
    }
}

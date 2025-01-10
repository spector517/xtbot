package com.github.spector517.xtbot.core.loader;

import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.*;

import com.github.spector517.xtbot.core.application.loader.BotClassLoader;

public class InternalClassLoader extends BotClassLoader {

    private final Set<Class<?>> botComponents;

    @SneakyThrows
    public InternalClassLoader() {
        this.botComponents = new HashSet<>();
        for (var className : getClassNames()) {
            botComponents.add(Class.forName(className, true, getParent()));
        }
    }

    @Override
    public Set<Class<?>> getBotComponents() {
        return botComponents;
    }

    @SneakyThrows
    private List<String> getClassNames() {
        var classNames = new ArrayList<String>();
        try(var res = getParent().getResourceAsStream("internal-components.txt")) {
            if (res == null) {
                throw new IllegalStateException("Resource not found: internal-components.txt");
            }
            try (var scanner = new Scanner(res, StandardCharsets.UTF_8)) {
                while (scanner.hasNextLine()) {
                    var line = scanner.nextLine();
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    classNames.add(line);
                }
            }
        }
        return classNames;
    }
}
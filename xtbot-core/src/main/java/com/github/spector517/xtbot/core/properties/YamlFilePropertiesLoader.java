package com.github.spector517.xtbot.core.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.nio.file.Files;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class YamlFilePropertiesLoader implements PropertiesLoader {

    private final String yamlPropsLocation;
    private final ObjectMapper yamlObjectMapper;

    private Properties properties;

    @Override
    public Properties load() throws LoadPropertiesException {
        if (properties != null) {
            return properties;
        }
        if (yamlPropsLocation == null || yamlPropsLocation.isBlank()) {
            throw new LoadPropertiesException("Properties path is undefined");
        }
        var configPath = Paths.get(yamlPropsLocation);
        if (!Files.exists(configPath)) {
            throw new LoadPropertiesException("Properties file not found: %s".formatted(configPath));
        }
        try {
            properties = yamlObjectMapper.readValue(configPath.toFile(), Properties.class);
        } catch (Exception e) {
            throw new LoadPropertiesException(e);
        }
        return properties;
    }
}

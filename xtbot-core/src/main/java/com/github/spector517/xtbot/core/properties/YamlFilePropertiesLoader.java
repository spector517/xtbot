package com.github.spector517.xtbot.core.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.properties.data.Properties;
import com.github.spector517.xtbot.core.properties.exception.LoadPropertiesException;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class YamlFilePropertiesLoader {

    private final String yamlPropsLocation;
    private final ObjectMapper yamlObjectMapper;

    private Properties properties;

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

        var includePreprocessor = new YamlFileIncludePreprocessor();
        try {
            var rawContent = Files.readString(configPath, StandardCharsets.UTF_8);
            var preprocessedContent = includePreprocessor.preprocess(rawContent, configPath);
            properties = yamlObjectMapper.readValue(preprocessedContent, Properties.class);
        } catch (Exception e) {
            throw new LoadPropertiesException(e);
        }
        return properties;
    }
}

package com.github.spector517.xtbot.core.properties;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YamlFileIncludePreprocessorTest {

    @Test
    @SneakyThrows
    void preprocess() {
        var expectedResult = Files.readString(
                Path.of("src/test/resources/preprocess/expected-result.yml"),
                StandardCharsets.UTF_8
        );
        var rootPath = Path.of("src/test/resources/preprocess/root.yml");
        var rootContent = Files.readString(rootPath, StandardCharsets.UTF_8);
        var actualResult = new YamlFileIncludePreprocessor().preprocess(rootContent, rootPath);
        assertEquals(expectedResult, actualResult);
    }
}
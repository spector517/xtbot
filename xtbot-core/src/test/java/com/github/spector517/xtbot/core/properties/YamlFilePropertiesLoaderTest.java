package com.github.spector517.xtbot.core.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class YamlFilePropertiesLoaderTest {

    private ObjectMapper yamlObjectMapper;

    private URL configURL;
    private URL invalidConfigURL;

    @BeforeEach
    void setUp() {
        yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        var configResPath = "properties/config.yaml";
        var invalidConfigPath = "properties/config_invalid.yaml";
        configURL = getClass().getClassLoader().getResource(configResPath);
        invalidConfigURL = getClass().getClassLoader().getResource(invalidConfigPath);
    }

    @Test
    @DisplayName("Fail: properties location is null or empty")
    void load_0() {
        var ex1 = assertThrows(
                LoadPropertiesException.class,
                () -> new YamlFilePropertiesLoader("", yamlObjectMapper).load()
        );
        var ex2 = assertThrows(
                LoadPropertiesException.class,
                () -> new YamlFilePropertiesLoader(null, yamlObjectMapper).load()
        );
        assertEquals("Properties path is undefined", ex1.getMessage());
        assertEquals("Properties path is undefined", ex2.getMessage());
    }

    @Test
    @DisplayName("Fail: properties location is not exists")
    void load_1() {
        var ex = assertThrows(
                LoadPropertiesException.class,
                () -> new YamlFilePropertiesLoader(
                        "not_exists_config.yaml", yamlObjectMapper
                ).load()
        );
        assertEquals("Properties file not found: not_exists_config.yaml", ex.getMessage());
    }

    @Test
    @DisplayName("Fail: Invalid properties format")
    void load_2() {
        var ex = assertThrows(
                LoadPropertiesException.class,
                () -> new YamlFilePropertiesLoader(invalidConfigURL.getPath(), yamlObjectMapper).load()
        );
        assertEquals(UnrecognizedPropertyException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("Success: Valid properties format")
    @SneakyThrows
    void load_3() {
        var expectedConfig = new Properties(
                1,
                "token",
                null,
                null,
                List.of(
                        new StageProps(
                                "init",
                                true,
                                null,
                                null,
                                null,
                                null,
                                List.of(new AcceptorProps("xtbot.internal.callback", "start")),
                                null,
                                null,
                                "start"
                        ),
                        new StageProps(
                                "start",
                                null,
                                true,
                                new MessageProps(
                                        null,
                                        "Text of message",
                                        ParseModeProps.PLAIN_TEXT,
                                        List.of(
                                                List.of(new ButtonProps(
                                                        "button1", "button1_data")
                                                ),
                                                List.of(new ButtonProps(
                                                        "button2", "button2_data")
                                                )
                                        )
                                ),
                                true,
                                null,
                                List.of(new AcceptorProps(
                                        "xtbot.internal.callback",
                                        "vpn-advantages|about-procedure|vps-rent|stop")),
                                List.of(new ActionProps(
                                                "executor",
                                                Map.of(
                                                        "arg1", "value1",
                                                        "arg2", "value2"
                                                ),
                                                "var_name"
                                        )
                                ),
                                Map.of("some_key", "some_value"),
                                null
                        )
                )
        );
        var actualConfig = new YamlFilePropertiesLoader(configURL.getPath(), yamlObjectMapper).load();
        assertEquals(expectedConfig, actualConfig);
    }

    @Test
    @DisplayName("Success: properties already loaded (singleton check)")
    @SneakyThrows
    void load_4() {
        var mapper = spy(yamlObjectMapper);
        var loader = new YamlFilePropertiesLoader(configURL.getPath(), mapper);
        loader.load();
        loader.load();
        verify(mapper, times(1))
                .readValue(any(File.class), eq(Properties.class));
    }
}

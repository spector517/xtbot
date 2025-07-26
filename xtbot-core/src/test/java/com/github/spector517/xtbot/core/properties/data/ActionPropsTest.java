package com.github.spector517.xtbot.core.properties.data;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionPropsTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Test
    @DisplayName("Success deserialization of action properties")
    @SneakyThrows
    void init_0() {
        try(var res =  getClass().getClassLoader().getResourceAsStream("properties/config.yaml")) {
            var props = mapper.readValue(res, Properties.class);
            var startStage = props.stages().get(1);
            assertEquals("executor", startStage.actions().getFirst().exec());
            assertEquals(
                    Map.of("arg1", "value1", "arg2", "value2"),
                    startStage.actions().getFirst().args()
            );
            assertEquals("var_name", startStage.actions().getFirst().register());
        }
    }

    @Test
    @DisplayName("More then one action in one item")
    @SneakyThrows
    void init_1() {
        try(var res =  getClass().getClassLoader()
                .getResourceAsStream("properties/config_several_actions.yaml")
        ) {
            var ex = assertThrows(
                    JsonMappingException.class,
                    () -> mapper.readValue(res, Properties.class)
            );
            assertTrue(ex.getMessage().startsWith(
                    "Executor 'executor1' already defined, cannot set one more 'executor2'"
            ));
        }
    }
}
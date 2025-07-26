package com.github.spector517.xtbot.core.properties.data;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcceptorPropsTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Test
    @DisplayName("Success deserialization of acceptor properties")
    @SneakyThrows
    void init_0() {
        try(var res =  getClass().getClassLoader().getResourceAsStream("properties/config.yaml")) {
            var props = mapper.readValue(res, Properties.class);
            var startStage = props.stages().get(1);
            assertEquals("x.accept.callback", startStage.accept().getFirst().acceptor());
            assertEquals(
                    "vpn-advantages|about-procedure|vps-rent|stop",
                    startStage.accept().getFirst().val()
            );
        }
    }

    @Test
    @DisplayName("Null value of acceptor value")
    @SneakyThrows
    void init_1() {
        try(var res =  getClass().getClassLoader().getResourceAsStream("properties/config_null_acceptor.yaml")) {
            var ex = assertThrows(
                   JsonMappingException.class,
                    () -> mapper.readValue(res, Properties.class)
            );
            assertTrue(ex.getMessage().startsWith("Value of acceptor 'some_acceptor' cannot be null"));
        }
    }

    @Test
    @DisplayName("More then one acceptor in one item")
    @SneakyThrows
    void init_2() {
        try(var res =  getClass().getClassLoader()
                .getResourceAsStream("properties/config_several_acceptors.yaml")
        ) {
            var ex = assertThrows(
                    JsonMappingException.class,
                    () -> mapper.readValue(res, Properties.class)
            );
            assertTrue(ex.getMessage().startsWith(
                    "Acceptor 'some_acceptor1' already defined, cannot set one more 'some_acceptor2'"
            ));
        }
    }
}
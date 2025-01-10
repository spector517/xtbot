package com.github.spector517.xtbot.core.application.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.spector517.xtbot.core.application.config.Template;
import com.github.spector517.xtbot.core.application.render.Render;

import lombok.SneakyThrows;

class CommonUtilsTest {

    private Render render;
    private Map<Object, Object> templatedMap;
    private Map<Object, Object> filledMap;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        render = mock(Render.class);
        filledMap = Map.of(
            "key1","value",
            "key2", Map.of(
                1, "val",
                "val", 2
            ),
            "key3", List.of("111", 222, "str")
        );
        templatedMap = Map.of(
            new Template(render, "key1"), new Template(render, "value"),
            new Template(render, "key2"), Map.of(
                1, new Template(render, "val"),
                new Template(render, "val"), 2
            ),
            new Template(render, "key3"), List.of(
                new Template(render, "111"),
                222,
                new Template(render, "str")
            )
        );
        when(render.render(anyString(), anyMap())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
    }

    @Test
    void testGetTemplatedMap() {
        var actualTemplatedMap = CommonUtils.getTemplatedMap(filledMap, render);
        assertEquals(templatedMap, actualTemplatedMap);
    }
    
    @Test
    void testGetFilledMap() {
        var actualFilledMap = CommonUtils.getFilledMap(templatedMap, Map.of());
        assertEquals(filledMap, actualFilledMap);
    }
}

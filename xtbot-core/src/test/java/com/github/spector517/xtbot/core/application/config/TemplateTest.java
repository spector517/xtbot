package com.github.spector517.xtbot.core.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.application.render.RenderException;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class TemplateTest {

    @Mock
    private Render render;

    @Test
    @DisplayName("Get value: valid template")
    @SneakyThrows
    void value_0() {
        var rawValue = "Hello {{ name }}!";
        var context = Map.of("name", (Object) "Alex");
        when(render.isTemplate(rawValue)).thenReturn(true);
        when(render.render(rawValue, context)).thenReturn("Hello Alex!");

        var template = new Template(render, rawValue);
        var result = template.value(context);

        assertEquals("Hello Alex!", result);
    }

    @Test
    @DisplayName("Get value: plain text")
    @SneakyThrows
    void value_1() {
        var rawValue = "Hello Alex!";
        var context = new HashMap<String, Object>();
        when(render.isTemplate(rawValue)).thenReturn(false);

        var template = new Template(render, rawValue);
        var result = template.value(context);

        assertEquals("Hello Alex!", result);
    }

    @Test
    @DisplayName("Get value: rendering failed")
    @SneakyThrows
    void value_2() {
        var rawValue = "Hello {% if name %}";
        var context = new HashMap<String, Object>();
        when(render.isTemplate(rawValue)).thenReturn(true);
        when(render.render(rawValue, context)).thenThrow(new RenderException("test"));

        var template = new Template(render, rawValue);
        var result = template.value(context);

        assertEquals("", result);
    }
}

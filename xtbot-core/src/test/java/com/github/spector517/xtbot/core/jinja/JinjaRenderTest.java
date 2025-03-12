package com.github.spector517.xtbot.core.jinja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.spector517.xtbot.core.application.render.RenderException;

import lombok.SneakyThrows;

class JinjaRenderTest {

    private final JinjaRender jinjaRender = new JinjaRender();

    @Test
    @DisplayName("Render: success")
    @SneakyThrows
    void render_0() {
        var template = "Hello {{ name }}!";
        var context = Map.of("name", (Object) "Alex");

        var result = jinjaRender.render(template, context);

        assertEquals("Hello Alex!", result);
    }

    @Test
    @DisplayName("Render: Invalid template")
    void render_1() {
        var template = "Hello {% if name.test %}!";
        var context = Map.of("name", (Object) "");
        assertThrows(RenderException.class, () -> jinjaRender.render(template, context));
    }

    @Test
    @DisplayName("Render: render with escape MarkdownV2 filter")
    @SneakyThrows
    void render_2() {
        var template = "Hello {{ name | escape_md2 }}!";
        var context = Map.of("name", (Object) "Alex*");

        var result = jinjaRender.render(template, context);

        assertEquals("Hello Alex\\*!", result);
    }

    @Test
    @DisplayName("Is template: yes")
    void isTemplate_0() {
        var template = "Hello {{ name }}!";

        assertTrue(jinjaRender.isTemplate(template));
    }

    @Test
    @DisplayName("Is template: no")
    void isTemplate_1() {
        var template = "Hello Alex!";

        assertFalse(jinjaRender.isTemplate(template));
    }

    @Test
    @DisplayName("Is template: null value")
    void isTemplate_2() {
        assertFalse(jinjaRender.isTemplate(null));
    }
}

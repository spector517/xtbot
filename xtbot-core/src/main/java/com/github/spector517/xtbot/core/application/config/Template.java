package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.application.render.RenderException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

@EqualsAndHashCode
@ToString
@Getter
@Accessors(fluent = true)
@Slf4j
public class Template {

    private final Render render;
    private final String rawValue;
    private final boolean needRender;

    public Template(Render render, String rawValue) {
        this.render = Objects.requireNonNull(render);
        this.rawValue = Objects.requireNonNullElse(rawValue, "");
        this.needRender = render.isTemplate(rawValue);
    }

    public String value(Map<String, Object> context) {
        if (needRender) {
            try {
                return render.render(rawValue, context);
            } catch (RenderException ex) {
                log.warn("Render error, using default value");
                log.debug("Exception occurred: ", ex);
                return "";
            }
        }
        return rawValue;
    }
}

package com.github.spector517.xtbot.core.jinja;

import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.application.render.RenderException;
import com.hubspot.jinjava.Jinjava;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class JinjaRender implements Render {

    private static final Pattern JINJA_FIND_PATTERN = Pattern.compile("\\{\\{.*}}");
    
    private Jinjava jinjava;

    public JinjaRender() {
        this.jinjava = new Jinjava();
    }

    @Override
    public String render(String template, Map<String, Object> context) throws RenderException {
        try {
            return jinjava.render(template, context);
        } catch (Exception ex) {
            log.warn("Render error: {}", ex.getMessage());
            log.debug("Exception occurred", ex);
            throw new RenderException(ex.getMessage());
        }
    }

    @Override
    public boolean isTemplate(String template) {
        return template != null && JINJA_FIND_PATTERN.matcher(template).find();
    }
}

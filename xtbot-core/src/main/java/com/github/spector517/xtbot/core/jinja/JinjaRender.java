package com.github.spector517.xtbot.core.jinja;

import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.application.render.RenderException;
import com.github.spector517.xtbot.core.jinja.filter.EscapeMd2Filter;
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
        this.jinjava.getGlobalContext().registerFilter(new EscapeMd2Filter());
    }

    @Override
    public String render(String template, Map<String, Object> context) throws RenderException {
        try {
            log.debug("Render '{}' with context '{}'", template, context);
            var res = jinjava.render(template, context);
            log.debug("Rendered successfully");
            return res;
        } catch (Exception ex) {
            log.warn("Render error: {}", ex.getMessage());
            log.debug("Exception occurred", ex);
            throw new RenderException(ex);
        }
    }

    @Override
    public boolean isTemplate(String template) {
        var res = template != null && JINJA_FIND_PATTERN.matcher(template).find();
        if (res) {
            log.debug("String '{}' is template", template);
        } else {
            log.debug("String '{}' is not template", template);
        }
        return res;
    }
}

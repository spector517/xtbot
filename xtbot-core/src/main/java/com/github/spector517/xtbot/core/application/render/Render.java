package com.github.spector517.xtbot.core.application.render;

import java.util.Map;

public interface Render {

    String render(String template, Map<String, Object> context) throws RenderException;

    boolean isTemplate(String template);
}

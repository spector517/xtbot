package com.github.spector517.xtbot.core.properties;


import java.util.List;
import java.util.Map;

public record StageProps(
        String name,
        Boolean initial,
        Boolean fail,
        MessageProps message,
        Boolean removeButtons,
        Boolean autocomplete,
        List<AcceptorProps> accept,
        List<ActionProps> actions,
        Map<String, Object> save,
        String next
) {}

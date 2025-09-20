package com.github.spector517.xtbot.core.properties.data;


import java.util.List;
import java.util.Map;

public record StageProps(
        String name,
        Boolean initial,
        Boolean fail,
        MessageProps message,
        Boolean removeButtons,
        Boolean autocomplete,
        Boolean sendTyping,
        List<AcceptorProps> accept,
        List<ActionProps> actions,
        Map<String, Object> save,
        String next
) {}

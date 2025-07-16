package com.github.spector517.xtbot.core.properties.data;

import java.util.Map;

public record ActionProps(
        String exec,
        Map<String, Object> args,
        String register
) {}

package com.github.spector517.xtbot.api.dto;

import java.util.List;

public record Command(
        int messageId,
        String name,
        List<String> args
) {}

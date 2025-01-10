package com.github.spector517.xtbot.core.properties;

import java.util.List;

public record Properties(
        int version,
        String botToken,
        Database database,
        String externalJarFilePath,
        List<StageProps> stages
) {}

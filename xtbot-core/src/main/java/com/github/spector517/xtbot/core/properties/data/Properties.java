package com.github.spector517.xtbot.core.properties.data;

import java.util.List;

public record Properties(
        int version,
        Database database,
        String externalJarFilePath,
        List<StageProps> stages
) {}

package com.github.spector517.xtbot.api.dto;

import java.util.List;
import java.util.Map;

public record Client(
        long externalId,
        String name,
        String stage,
        boolean currentStageInitiated,
        boolean currentStageCompleted,
        List<String> previousStages,
        Map<String, Object> additionalVars,
        Map<String, Object> stageVars
) {}

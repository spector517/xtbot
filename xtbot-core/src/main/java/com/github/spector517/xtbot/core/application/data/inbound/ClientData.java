package com.github.spector517.xtbot.core.application.data.inbound;

import lombok.Data;
import lombok.experimental.Accessors;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@Accessors(fluent = true, chain = true)
public class ClientData {

    public static final String EXTERNAL_ID_KEY = "externalId";
    public static final String CURRENT_STAGE_KEY = "stage";

    private long id;
    private long externalId;
    private String name;
    private String currentStage;
    private boolean currentStageInitiated;
    private boolean currentStageCompleted;
    private int previousSendedMessageId;
    private List<String> previousStages;
    private Map<String, Object> additionalVars;
    private Map<String, Object> stageVars;

    public Optional<String> getPreviousStage() {
        return previousStages.isEmpty()
            ? Optional.empty()
            : Optional.of(previousStages.getLast());
    }

    public void registerCompletedStage(String stageName) {
        previousStages = new ArrayList<>(previousStages);
        previousStages.add(stageName);
    }

    public ClientData externalId(long externalId) {
        this.externalId = externalId;
        if (externalId <= 0) {
            MDC.remove(EXTERNAL_ID_KEY);
            return this;
        }
        MDC.put(EXTERNAL_ID_KEY, String.valueOf(externalId));
        return this;
    }

    public ClientData currentStage(String currentStage) {
        this.currentStage = currentStage;
        if (currentStage == null || currentStage.isEmpty()) {
            MDC.remove(CURRENT_STAGE_KEY);
            return this;
        }
        MDC.put(CURRENT_STAGE_KEY, currentStage);
        return this;
    }
}

package com.github.spector517.xtbot.core.application.data.inbound;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@Accessors(fluent = true, chain = true)
@NoArgsConstructor
public class ClientData {

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
            : Optional.of(previousStages.get(previousStages.size() - 1));
    }

    public void registerCompletedStage(String stageName) {
        previousStages = new ArrayList<>(previousStages);
        previousStages.add(stageName);
    }
}

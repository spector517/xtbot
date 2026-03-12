package com.github.spector517.xtbot.core.application.data.inbound;

import lombok.*;
import lombok.experimental.Accessors;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.*;

@SuppressWarnings("UnusedReturnValue")
@Accessors(fluent = true, chain = true)
@EqualsAndHashCode
@ToString
public class ClientData {

    public static final String EXTERNAL_ID_KEY = "externalId";
    public static final String CURRENT_STAGE_KEY = "stage";

    @Getter
    private long externalId;
    @Getter
    @Setter
    private String name;
    @Getter
    private String stageName;
    @Getter
    private boolean stageInitiated;
    @Getter
    private boolean stageCompleted;
    private List<String> previousStages;
    private List<ChatMessage> messages;
    private Map<String, Object> additionalVars;
    private Map<String, Object> stageVars;

    public ClientData() {
        previousStages = new ArrayList<>();
        messages = new ArrayList<>();
        additionalVars = new HashMap<>();
        stageVars = new HashMap<>();
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

    public ClientData previousStages(List<String> previousStages) {
        this.previousStages = previousStages != null
                ? new ArrayList<>(previousStages)
                : new ArrayList<>();
        return this;
    }

    public List<String> previousStages() {
        return previousStages != null
                ? List.copyOf(previousStages)
                : List.of();
    }

    public ClientData bindNewStage(@NonNull String nextStageName) {
        if (stageName != null) {
            previousStages.add(stageName);
        }
        stageName = nextStageName;
        stageInitiated = false;
        stageCompleted = false;
        if (nextStageName.isEmpty()) {
            MDC.remove(CURRENT_STAGE_KEY);
            return this;
        }
        MDC.put(CURRENT_STAGE_KEY, nextStageName);
        return this;
    }

    public Optional<String> getPreviousStage() {
        return previousStages.isEmpty()
                ? Optional.empty()
                : Optional.of(previousStages.getLast());
    }

    public ClientData setStageInitiated() {
        this.stageInitiated = true;
        return this;
    }

    public ClientData setStageCompleted() {
        this.stageCompleted = true;
        return this;
    }

    // Derived from messages: all BOT messages with non-null telegramMessageId
    public List<Integer> sentMessageIds() {
        return messages.stream()
                .filter(m -> m.type() == MessageType.BOT && m.telegramMessageId() != null)
                .map(ChatMessage::telegramMessageId)
                .toList();
    }

    public Optional<Integer> getPreviousSentMessageId() {
        var botIds = sentMessageIds();
        return botIds.isEmpty()
                ? Optional.empty()
                : Optional.of(botIds.getLast());
    }

    public ClientData registerBotMessage(int messageId, String text) {
        messages.add(new ChatMessage(messageId, text, LocalDateTime.now(), MessageType.BOT));
        return this;
    }

    public ClientData registerUserMessage(Integer messageId, String text) {
        messages.add(new ChatMessage(messageId, text, LocalDateTime.now(), MessageType.USER));
        return this;
    }

    public ClientData registerMessage(ChatMessage message) {
        messages.add(message);
        return this;
    }

    public ClientData messages(List<ChatMessage> messages) {
        this.messages = messages != null
                ? new ArrayList<>(messages)
                : new ArrayList<>();
        return this;
    }

    public List<ChatMessage> messages() {
        return messages != null
                ? List.copyOf(messages)
                : List.of();
    }

    public ClientData additionalVars(Map<String, Object> additionalVars) {
        this.additionalVars = additionalVars != null
                ? new HashMap<>(additionalVars)
                : new HashMap<>();
        return this;
    }

    public Map<String, Object> additionalVars() {
        return additionalVars != null
                ? Map.copyOf(additionalVars)
                : Map.of();
    }

    public ClientData updateAdditionalVars(Map<String, Object> updateVars) {
        additionalVars.putAll(updateVars);
        return this;
    }

    public ClientData stageVars(Map<String, Object> stageVars) {
        this.stageVars = stageVars != null
                ? new HashMap<>(stageVars)
                : new HashMap<>();
        return this;
    }

    public Map<String, Object> stageVars() {
        return stageVars != null
                ? Map.copyOf(stageVars)
                : Map.of();
    }

    public ClientData updateStageVars(Map<String, Object> updateVars) {
        stageVars.putAll(updateVars);
        return this;
    }
}

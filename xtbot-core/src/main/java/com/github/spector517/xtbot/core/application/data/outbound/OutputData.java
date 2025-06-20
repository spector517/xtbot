package com.github.spector517.xtbot.core.application.data.outbound;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(fluent = true, chain = true)
public class OutputData {

    @NonNull
    private final Long chatId;
    @NonNull
    private final OutputType type;

    private Integer messageId;
    private Integer deleteMessageId;
    private String text;
    private String parseMode;
    private List<List<Button>> buttons;
    private Integer previousSentMessageId;

    public record Button(String display, String data) {}
}

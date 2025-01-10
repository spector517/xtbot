package com.github.spector517.xtbot.core.application.data.outbound;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class OutputData {

    private long chatId;
    private String text;
    private String parseMode;
    private List<List<Button>> buttons = List.of();
    private boolean removeButtons;
    private int previousSendedMessageId;
    
    @Data
    @Accessors(fluent = true, chain = true)
    public static class Button {
        
        private String display;
        private String data;
    }
}

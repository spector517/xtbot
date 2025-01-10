package com.github.spector517.xtbot.core.application.data.inbound;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class CallbackData {

    private String data;
}

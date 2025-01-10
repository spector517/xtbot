package com.github.spector517.xtbot.core.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.spector517.xtbot.core.application.data.inbound.CallbackData;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter(onMethod = @__(@JsonProperty))
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor
public class CallbackContext {

    private final String data;

    public CallbackContext(CallbackData callbackData) {
        this.data = callbackData.data();
    }
}

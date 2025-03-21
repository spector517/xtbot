package com.github.spector517.xtbot.core.properties;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum ParseModeProps {
    @JsonProperty("markdown") MARKDOWN,
    @JsonProperty("markdown_v2") MARKDOWN_V2,
    @JsonProperty("plain") PLAIN_TEXT
}

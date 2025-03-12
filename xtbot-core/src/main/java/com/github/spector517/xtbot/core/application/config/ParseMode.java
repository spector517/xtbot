package com.github.spector517.xtbot.core.application.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum ParseMode {

    MARKDOWN("Markdown"),
    MARKDOWN_V2("MarkdownV2"),
    PLAIN_TEXT(null);

    private final String type;
}

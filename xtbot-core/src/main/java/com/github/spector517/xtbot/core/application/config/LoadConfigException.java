package com.github.spector517.xtbot.core.application.config;

class LoadConfigException extends RuntimeException {

    LoadConfigException(Throwable e) {
        super(e);
    }

    LoadConfigException(String message) {
        super(message);
    }
}

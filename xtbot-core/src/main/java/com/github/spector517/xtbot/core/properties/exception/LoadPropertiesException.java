package com.github.spector517.xtbot.core.properties.exception;

public class LoadPropertiesException extends Exception {

    public LoadPropertiesException(Throwable ex) {
        super(ex);
    }

    public LoadPropertiesException(String message) {
        super(message);
    }
}

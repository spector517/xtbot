package com.github.spector517.xtbot.core.mapper;

public class MappingException extends Exception {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(Throwable cause) {
        super(cause);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.github.spector517.xtbot.core.mapper;

@FunctionalInterface
public interface Mapper<S, T> {
    S map(T t, Object... options) throws MappingException;
}

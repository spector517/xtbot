package com.github.spector517.xtbot.core.application.mapper;

@FunctionalInterface
public interface Mapper<T, S> {
    S map(T t) throws MappingException;
}

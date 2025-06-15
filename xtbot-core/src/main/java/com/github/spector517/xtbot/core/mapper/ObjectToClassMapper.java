package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObjectToClassMapper implements Mapper<Object, Object> {

    private final ObjectMapper objectMapper;

    @Override
    public Object map(Object object, Object... options) throws MappingException {
        try {
            var clazz = (Class<?>) options[0];
            return objectMapper.convertValue(object, clazz);
        } catch (Exception e) {
            throw new MappingException("Failed to map object to class %s".formatted(options[0]), e);
        }
    }
}

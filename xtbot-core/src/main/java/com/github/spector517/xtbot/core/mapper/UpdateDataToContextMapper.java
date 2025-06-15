package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.context.Context;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class UpdateDataToContextMapper implements Mapper<Map<String, Object>, UpdateData> {

    private final ObjectMapper mapper;

    @Override
    public Map<String, Object> map(UpdateData updateData, Object... ignored) throws MappingException {
        var context = new Context(updateData);
        try {
            return mapper.convertValue(context, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new MappingException(ex);
        }
    }
}

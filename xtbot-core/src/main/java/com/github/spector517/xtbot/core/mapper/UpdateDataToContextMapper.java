package com.github.spector517.xtbot.core.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.mapper.MappingException;
import com.github.spector517.xtbot.core.context.Context;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class UpdateDataToContextMapper implements Mapper<UpdateData, Map<String, Object>> {

    private final ObjectMapper mapper;

    @Override
    public Map<String, Object> map(UpdateData updateData) throws MappingException {
        var context = new Context(updateData);
        try {
            return mapper.convertValue(context, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new MappingException(ex);
        }
    }
}

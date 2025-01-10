package com.github.spector517.xtbot.core.application.component;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.application.repository.ClientEntity;
import com.github.spector517.xtbot.core.application.repository.ClientRepository;
import com.github.spector517.xtbot.core.properties.PropertiesLoader;

public interface ComponentsContainer {

    ObjectMapper yamlObjectMapper();

    ObjectMapper jsonObjectMapper();

    AcceptorChecker acceptorChecker();

    ExecutorChecker executorChecker();

    AcceptorLoader acceptorLoader();

    ExecutorLoader executorLoader();

    Render render();

    Mapper<ClientData, ClientEntity> clientDataToEntityMapper();

    Mapper<ClientEntity, ClientData> clientEntityToDataMapper();

    <A> Mapper<UpdateData, A> updateDataToApiMapper();

    Mapper<UpdateData, Map<String, Object>> updateDataToContextMapper();

    <I> Mapper<I, UpdateData> tgSdkUpdateToDataMapper();

    PropertiesLoader propertiesLoader();

    ClientRepository clientRepository();

    String botToken();

    String botUsername();
}

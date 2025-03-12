package com.github.spector517.xtbot.core.container;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.data.inbound.ClientData;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.CommonMethodsLoader;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.mapper.ClientDataToEntityMapper;
import com.github.spector517.xtbot.core.application.mapper.ClientEntityToDataMapper;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.application.repository.ClientEntity;
import com.github.spector517.xtbot.core.application.repository.ClientRepository;
import com.github.spector517.xtbot.core.jinja.JinjaRender;
import com.github.spector517.xtbot.core.loader.ExternalJarClassLoader;
import com.github.spector517.xtbot.core.loader.InternalClassLoader;
import com.github.spector517.xtbot.core.mapper.TgSdkUpdateToDataMapper;
import com.github.spector517.xtbot.core.mapper.UpdateDataToBotApiMapper;
import com.github.spector517.xtbot.core.mapper.UpdateDataToContextMapper;
import com.github.spector517.xtbot.core.properties.LoadPropertiesException;
import com.github.spector517.xtbot.core.properties.Properties;
import com.github.spector517.xtbot.core.properties.PropertiesLoader;
import com.github.spector517.xtbot.core.properties.YamlFilePropertiesLoader;
import com.github.spector517.xtbot.core.repository.InternalClientRepository;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class DefaultComponentsContainer implements ComponentsContainer {

    private final ObjectMapper yamlObjectMapper;
    private final ObjectMapper jsonObjectMapper;

    private final PropertiesLoader propertiesLoader;
    private final AcceptorChecker acceptorChecker;
    private final ExecutorChecker executorChecker;
    private final AcceptorLoader acceptorLoader;
    private final ExecutorLoader executorLoader;
    private final Render render;
    private final ClientRepository clientRepository;
    private final Mapper<ClientEntity, ClientData> clientEntityToDataMapper;
    private final Mapper<ClientData, ClientEntity> clientDataToEntityMapper;
    private final Mapper<UpdateData, Update> updateDataToApiMapper;
    private final Mapper<org.telegram.telegrambots.meta.api.objects.Update, UpdateData> tgSdkUpdateToDataMapper;
    private final Mapper<UpdateData, Map<String, Object>> updateDataToContextMapper;
    private final String botToken;
    private final String botUsername;


    public DefaultComponentsContainer(String propertiesFileLocation) {
        jsonObjectMapper = new ObjectMapper();
        yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        propertiesLoader = new YamlFilePropertiesLoader(propertiesFileLocation, yamlObjectMapper);
        final Properties props;
        try {
            props = propertiesLoader.load();
        } catch(LoadPropertiesException e) {
            throw new ComponentsCreationException(e);
        }

        acceptorChecker = AcceptorChecker.getAcceptorChecker(props.version());
        executorChecker = ExecutorChecker.getExecutorChecker(props.version());
        final CommonMethodsLoader commonMethodsLoader;
        if (props.externalJarFilePath() != null) {
            commonMethodsLoader = new CommonMethodsLoader(
                    new InternalClassLoader(),
                    new ExternalJarClassLoader(props.externalJarFilePath())
            );
        } else {
            commonMethodsLoader = new CommonMethodsLoader(new InternalClassLoader());
        }
        acceptorLoader = commonMethodsLoader;
        executorLoader = commonMethodsLoader;
        render = new JinjaRender();
        clientRepository = new InternalClientRepository();
        clientEntityToDataMapper = new ClientEntityToDataMapper(jsonObjectMapper);
        clientDataToEntityMapper = new ClientDataToEntityMapper(jsonObjectMapper);
        updateDataToApiMapper = new UpdateDataToBotApiMapper();
        updateDataToContextMapper = new UpdateDataToContextMapper(jsonObjectMapper);
        tgSdkUpdateToDataMapper = new TgSdkUpdateToDataMapper(clientRepository, clientEntityToDataMapper);

        botToken = props.botToken();
        botUsername = "XTBot";
    }
}

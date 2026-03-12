package com.github.spector517.xtbot.telegram.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.spector517.xtbot.core.application.extension.CommonMethodsLoader;
import com.github.spector517.xtbot.core.jinja.JinjaRender;
import com.github.spector517.xtbot.core.loader.InternalClassLoader;
import com.github.spector517.xtbot.core.mapper.*;
import com.github.spector517.xtbot.core.properties.YamlFilePropertiesLoader;
import com.github.spector517.xtbot.core.properties.data.Properties;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import com.github.spector517.xtbot.core.repository.InternalClientRepository;
import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.telegram.mapper.TgSdkUpdateToDataMapper;
import lombok.SneakyThrows;
import org.telegram.telegrambots.client.AbstractTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelegramSdkApiBotStressTest {

    public static void main(String... args) {
        var bot = createBot();
    }

    private static TelegramSdkApiBot createBot() {
        var clientRepository = createClientRepository();
        var yamlObjectMapper = new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var jsonObjectMapper = new ObjectMapper();
        var parameters = new TelegramSdkApiBot.Parameters()
                .telegramClient(createTelegramClient())
                .executorService(Executors.newVirtualThreadPerTaskExecutor())
                .clientRepository(clientRepository)
                .properties(createProperties(yamlObjectMapper))
                .sdkMapper(createSdkMapper(clientRepository, jsonObjectMapper))
                .contextMapper(new UpdateDataToContextMapper(jsonObjectMapper))
                .apiMapper(new UpdateDataToBotApiMapper())
                .actionResultMapper(new ObjectToClassMapper(jsonObjectMapper))
                .toEntityMapper(new ClientDataToEntityMapper(jsonObjectMapper))
                .render(new JinjaRender())
                .commonMethodsLoader(new CommonMethodsLoader(new InternalClassLoader()))
                .token("token");
        return new TelegramSdkApiBot(parameters);
    }

    @SneakyThrows
    private static AbstractTelegramClient createTelegramClient() {
        var client = mock(AbstractTelegramClient.class);
        var message = mock(Message.class);
        when(message.getMessageId()).thenReturn(new Random().nextInt());
        when(client.execute(any(SendMessage.class))).then(invocationOnMock -> {
            sleepMillis(100);
            return message;
        });
        when(client.execute(any(EditMessageText.class))).then(invocationOnMock -> {
            sleepMillis(100);
            return mock(Serializable.class);
        });
        when(client.execute(any(EditMessageReplyMarkup.class))).then(invocationOnMock -> {
            sleepMillis(100);
            return mock(Serializable.class);
        });
        return client;
    }

    @SneakyThrows
    private static ClientRepository createClientRepository() {
        var repository = spy(new InternalClientRepository());
        doAnswer(invocationOnMock -> {
            sleepMillis(50);
            return invocationOnMock.callRealMethod();
        }).when(repository).findByExternalId(anyLong());
        doAnswer(invocationOnMock -> {
            sleepMillis(50);
            return invocationOnMock.callRealMethod();
        }).when(repository).save(any(ClientEntity.class));
        return repository;
    }

    @SneakyThrows
    private static Properties createProperties(ObjectMapper yamlObjectMapper) {
        return new YamlFilePropertiesLoader(
                "xtbot-telegram/src/test/resources/stress-props.yml",
                yamlObjectMapper
        ).load();
    }

    private static TgSdkUpdateToDataMapper createSdkMapper(
            ClientRepository clientRepository,
            ObjectMapper objectMapper
    ) {
        return new TgSdkUpdateToDataMapper(
                clientRepository,
                new ClientEntityToDataMapper(objectMapper),
                "init"
        );
    }

    @SneakyThrows
    private static void sleepMillis(long millis) {
        Thread.sleep(millis);
    }
}
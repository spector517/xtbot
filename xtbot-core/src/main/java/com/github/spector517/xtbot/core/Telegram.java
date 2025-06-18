package com.github.spector517.xtbot.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.spector517.xtbot.core.application.extension.CommonMethodsLoader;
import com.github.spector517.xtbot.core.jinja.JinjaRender;
import com.github.spector517.xtbot.core.loader.ExternalJarClassLoader;
import com.github.spector517.xtbot.core.loader.InternalClassLoader;
import com.github.spector517.xtbot.core.mapper.*;
import com.github.spector517.xtbot.core.properties.*;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import com.github.spector517.xtbot.core.repository.H2ClientRepository;
import com.github.spector517.xtbot.core.repository.InternalClientRepository;
import com.github.spector517.xtbot.core.telegram.api.sdk.TelegramSdkApiBot;
import com.github.spector517.xtbot.core.telegram.api.auth.PropsBotAuthLoader;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.nio.file.Path;
import java.util.concurrent.Executors;

@Slf4j
public class Telegram {

    public static final String VERSION = "0.3.0";

    public static void main(String... args) {
        log.info("Starting XTBot v{} ...", VERSION);
        checkArguments(args);
        var yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var properties = loadProperties(args[0], yamlObjectMapper);
        var bot = createBot(properties);
        registerBot(bot);
        log.info("XTBot successfully started.");
    }

    private static void checkArguments(String... args) {
        if (args.length != 1) {
            log.error("Invalid number of arguments.");
            log.error("Usage: java -jar xtbot-core-{} <config.yml>", VERSION);
            System.exit(-1);
        }
    }

    private static Properties loadProperties(String yamlPropsLocation, ObjectMapper yamlObjectMapper) {
        try {
            return new YamlFilePropertiesLoader(yamlPropsLocation, yamlObjectMapper).load();
        } catch (LoadPropertiesException e) {
            log.error("Failed to load properties from {}: {}", yamlPropsLocation, e.getMessage());
            log.debug("Stack trace", e);
            System.exit(-2);
            return null;
        }
    }

    private static TelegramSdkApiBot createBot(Properties properties) {
        try {
            var initialStageName = properties.stages().stream()
                    .filter(StageProps::initial)
                    .findAny().orElseThrow(() ->
                            new IllegalStateException("No initial stage found")
                    )
                    .name();
            var objectMapper = new ObjectMapper();
            var repository = createRepository(properties);
            var sdkMapper = new TgSdkUpdateToDataMapper(
                    repository,
                    new ClientEntityToDataMapper(objectMapper),
                    initialStageName
            );
            var commonMethodsLoader = properties.externalJarFilePath() == null || properties.externalJarFilePath().isBlank()
                    ? new CommonMethodsLoader(new InternalClassLoader())
                    : new CommonMethodsLoader(
                    new InternalClassLoader(), new ExternalJarClassLoader(properties.externalJarFilePath()
            ));
            var parameters = new TelegramSdkApiBot.Parameters()
                    .botAuthLoader(new PropsBotAuthLoader(properties))
                    .executorService(Executors.newVirtualThreadPerTaskExecutor())
                    .clientRepository(repository)
                    .properties(properties)
                    .sdkMapper(sdkMapper)
                    .contextMapper(new UpdateDataToContextMapper(objectMapper))
                    .apiMapper(new UpdateDataToBotApiMapper())
                    .actionResultMapper(new ObjectToClassMapper(objectMapper))
                    .toEntityMapper(new ClientDataToEntityMapper(objectMapper))
                    .render(new JinjaRender())
                    .commonMethodsLoader(commonMethodsLoader);
            return new TelegramSdkApiBot(parameters);
        } catch (Exception e) {
            log.error("Failed to create bot: {}", e.getMessage());
            log.debug("Stack trace", e);
            System.exit(-3);
            return null;
        }
    }

    private static void registerBot(TelegramSdkApiBot bot) {
        try {
            log.info("Registering bot in Telegram...");
            var botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            log.info("Bot registered successfully.");
        } catch (TelegramApiException e) {
            log.error("Failed to register bot: {}", e.getMessage());
            log.debug("Stack trace", e);
            System.exit(-4);
        }
    }

    private static ClientRepository createRepository(Properties properties) {
        if (properties.database() == null) {
            return new InternalClientRepository();
        }
        return properties.database().type() == DatabaseType.H2
                ? new H2ClientRepository(Path.of(properties.database().h2().directory()))
                : new InternalClientRepository();
    }
}
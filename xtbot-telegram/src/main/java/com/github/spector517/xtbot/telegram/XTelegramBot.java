package com.github.spector517.xtbot.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.spector517.xtbot.core.application.extension.CommonMethodsLoader;
import com.github.spector517.xtbot.core.jinja.JinjaRender;
import com.github.spector517.xtbot.core.loader.ExternalJarClassLoader;
import com.github.spector517.xtbot.core.loader.InternalClassLoader;
import com.github.spector517.xtbot.core.mapper.*;
import com.github.spector517.xtbot.core.properties.YamlFilePropertiesLoader;
import com.github.spector517.xtbot.core.properties.data.DatabaseType;
import com.github.spector517.xtbot.core.properties.data.Properties;
import com.github.spector517.xtbot.core.properties.data.StageProps;
import com.github.spector517.xtbot.core.properties.exception.LoadPropertiesException;
import com.github.spector517.xtbot.core.repository.ClientRepository;
import com.github.spector517.xtbot.core.repository.H2ClientRepository;
import com.github.spector517.xtbot.core.repository.InternalClientRepository;
import com.github.spector517.xtbot.telegram.mapper.TgSdkUpdateToDataMapper;
import com.github.spector517.xtbot.telegram.sdk.TelegramSdkApiBot;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Path;
import java.util.concurrent.Executors;

@Slf4j
public class XTelegramBot {

    private static final String VERSION = "0.4.0";
    private static final String ENV_VAR_TOKEN_NAME = "X_TELEGRAM_TOKEN";

    public static void main(String... args) {
        log.info("Starting XTBot v{} ...", VERSION);
        checkArguments(args);
        var token = getToken();

        var yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var properties = loadProperties(args[0], yamlObjectMapper);


        var bot = createBot(properties, token);
        registerAndRunBot(bot);
    }

    private static void checkArguments(String... args) {
        if (args.length != 1) {
            log.error("Invalid number of arguments.");
            log.error("Usage: java -jar xtbot-core-{} <properties.yml>", VERSION);
            System.exit(101);
        }
    }

    private static String getToken() {
        var token = System.getenv(ENV_VAR_TOKEN_NAME);
        if (token == null) {
            log.error("Missing Telegram token.");
            log.error("Create environment variable {}=<telegram_token>", ENV_VAR_TOKEN_NAME);
            System.exit(102);
        }
        return token;
    }

    private static Properties loadProperties(String yamlPropsLocation, ObjectMapper yamlObjectMapper) {
        log.info("Loading bot configuration...");
        try {
            var properties = new YamlFilePropertiesLoader(yamlPropsLocation, yamlObjectMapper).load();
            log.info("Configuration loaded successfully.");
            return properties;
        } catch (LoadPropertiesException e) {
            log.error("Failed to load properties from {}: {}", yamlPropsLocation, e.getMessage());
            logException(e);
            System.exit(103);
            return null;
        }

    }

    private static TelegramSdkApiBot createBot(Properties properties, String token) {
        log.info("Creating bot...");
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
                    .telegramClient(new OkHttpTelegramClient(token))
                    .executorService(Executors.newVirtualThreadPerTaskExecutor())
                    .clientRepository(repository)
                    .properties(properties)
                    .sdkMapper(sdkMapper)
                    .contextMapper(new UpdateDataToContextMapper(objectMapper))
                    .apiMapper(new UpdateDataToBotApiMapper())
                    .actionResultMapper(new ObjectToClassMapper(objectMapper))
                    .toEntityMapper(new ClientDataToEntityMapper(objectMapper))
                    .render(new JinjaRender())
                    .commonMethodsLoader(commonMethodsLoader)
                    .token(token);
            var bot = new TelegramSdkApiBot(parameters);
            log.info("Bot created successfully.");
            return bot;
        } catch (Exception e) {
            log.error("Failed to create bot: {}", e.getMessage());
            logException(e);
            System.exit(104);
            return null;
        }
    }

    private static void registerAndRunBot(TelegramSdkApiBot bot) {
        try(var botApplication = new TelegramBotsLongPollingApplication()) {
            log.info("Registering bot in Telegram...");
            botApplication.registerBot(bot.token(), bot);
            log.info("XTBot successfully registered and started.");
            Thread.currentThread().join();
        } catch (TelegramApiException e) {
            log.error("Failed to register bot: {}", e.getMessage());
            logException(e);
            System.exit(105);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Unknown Telegram SDK error: {}", e.getMessage());
            logException(e);
            System.exit(106);
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

    private static void logException(Exception e) {
        log.debug("Stack trace", e);
    }
}
package com.github.spector517.xtbot.core;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.github.spector517.xtbot.core.api.sdk.TelegramSdkApiBot;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.container.DefaultComponentsContainer;
import com.github.spector517.xtbot.core.mapper.TgSdkUpdateToDataMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String VERSION = "0.2.0";
    
    public static void main(String[] args) {
        log.info("Starting XTBot v%s ...".formatted(VERSION));

        if (args.length != 1) {
            log.error("Invalid number of arguments.");
            log.error("Usage: java -jar xtbot-core-%s <config.yml>".formatted(VERSION));
            System.exit(1);
        }

        var container = new DefaultComponentsContainer(args[0]);
        var config = new Config(container);
        var sdkMapper = (TgSdkUpdateToDataMapper) container.tgSdkUpdateToDataMapper();
        sdkMapper.initialStageName(config.initialStage().name());
        var bot = new TelegramSdkApiBot(config);
        final TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            log.info("Registering bot in Telegram...");
            botsApi.registerBot(bot);
            log.info("Bot registered.");
        } catch (TelegramApiException ex) {
            log.error("Failed to register bot: {}", ex.getMessage());
            log.debug("Exception occurred", ex);
            System.exit(1);
        }

        log.info("XTBot successfully started.");
    }
}
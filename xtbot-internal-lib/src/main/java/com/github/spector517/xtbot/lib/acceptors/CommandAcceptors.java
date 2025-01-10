package com.github.spector517.xtbot.lib.acceptors;

import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.dto.Update;
import lombok.experimental.UtilityClass;

@BotComponent
@UtilityClass
public class CommandAcceptors {

    @Acceptor("xtbot.internal.command")
    public boolean isAccepted(Update update, String val) {
        if (update.message() == null) {
            return false;
        }
        if (update.message().text().startsWith("/")) {
            var command = update.message().text().substring(1);
            return command.equals(val);
        }
        return false;
    }
}

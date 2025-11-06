package com.github.spector517.xtbot.lib.acceptors;

import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.dto.Update;
import lombok.experimental.UtilityClass;

@BotComponent
@UtilityClass
public class CommandAcceptor {

    @Acceptor("x.accept.command")
    public boolean isAccepted(Update update, String val) {
        if (update.command() == null) {
            return false;
        }
        return update.command().name().equals(val);
    }
}

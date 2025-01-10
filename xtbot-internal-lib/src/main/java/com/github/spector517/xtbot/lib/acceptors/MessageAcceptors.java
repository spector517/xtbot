package com.github.spector517.xtbot.lib.acceptors;

import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.dto.Update;
import lombok.experimental.UtilityClass;

@BotComponent
@UtilityClass
public class MessageAcceptors {

    @Acceptor("xtbot.internal.message")
    public boolean isAccepted(Update update, String val) {
        if (update.message() == null) {
            return false;
        }
        return update.message().text().matches(val);
    }
}

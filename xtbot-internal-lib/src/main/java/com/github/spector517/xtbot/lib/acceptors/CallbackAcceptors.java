package com.github.spector517.xtbot.lib.acceptors;

import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.dto.Update;
import lombok.experimental.UtilityClass;

@BotComponent
@UtilityClass
public class CallbackAcceptors {

    @Acceptor("xtbot.internal.callback")
    public boolean isAccepted(Update update, String val) {
        if (update.callback() == null) {
            return false;
        }
        return val.equals(update.callback().data());
    }
}

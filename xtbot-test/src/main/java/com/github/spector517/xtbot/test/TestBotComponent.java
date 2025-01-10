package com.github.spector517.xtbot.test;

import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.annotation.Executor;
import lombok.experimental.UtilityClass;

@BotComponent
@UtilityClass
public class TestBotComponent {

    @Executor("test")
    public String test1() {
        return "test";
    }

    @Acceptor("test")
    public boolean test2() {
        return true;
    }
}

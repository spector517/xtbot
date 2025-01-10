package com.github.spector517.xtbot.core.common;

import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.api.annotation.Name;
import com.github.spector517.xtbot.api.dto.Update;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestComponent {

    @Executor("exec1")
    public static void execute(
            @Name("name") String name,
            Integer age
    ) {
        throw new UnsupportedOperationException("Unsupported call test executor");
    }

    @Executor("exec2")
    public static void execute(
            @Name("radius") int radius,
            @Name("strict") boolean strict
    ) {
        throw new UnsupportedOperationException("Unsupported call test executor");
    }

    @Executor("exec3")
    public static void execute(
            @Name("users") List<String> users,
            Map<String, Object> data
    ) {
        throw new UnsupportedOperationException("Unsupported call test executor");
    }

    @Executor("exec4")
    public static void execute(
            String name,
            Integer age,
            boolean isMale,
            List<String> cars,
            Map<String, Object> data
    ) {}

    @Executor("exec5")
    private static void execute(
            String test
    ) {
        throw new UnsupportedOperationException("Unsupported call test executor");
    }

    @Executor("exec6")
    public void execute(
            Integer test
    ) {
        throw new UnsupportedOperationException("Unsupported call test executor");
    }


    @Acceptor("acc1")
    public void accept(Update update, String val) {
        throw new UnsupportedOperationException("Unsupported call test acceptor");
    }
}
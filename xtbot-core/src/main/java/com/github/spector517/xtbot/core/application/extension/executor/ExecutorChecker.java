package com.github.spector517.xtbot.core.application.extension.executor;

import java.lang.reflect.Method;
import java.util.Map;

public interface ExecutorChecker {

    static ExecutorChecker getExecutorChecker(int version) {
        if (version == 1) {
            return new ExecutorCheckerV1();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported version of ComponentChecker: ".concat(String.valueOf(version))
            );
        }
    }

    void checkExecutor(Method method, Map<String, Object> arguments) throws ExecutorCheckFailedException;
}

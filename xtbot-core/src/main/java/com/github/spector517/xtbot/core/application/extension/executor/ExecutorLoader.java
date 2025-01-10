package com.github.spector517.xtbot.core.application.extension.executor;

import java.lang.reflect.Method;

public interface ExecutorLoader {

    Method getExecutor(String name) throws ExecutorNotFoundException;
}

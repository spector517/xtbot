package com.github.spector517.xtbot.core.application.extension.executor;

import java.lang.reflect.Parameter;

public interface ParameterNameDetector {

    String getParameterName(Parameter parameter);
}

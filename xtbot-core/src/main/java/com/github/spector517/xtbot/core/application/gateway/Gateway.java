package com.github.spector517.xtbot.core.application.gateway;

import com.github.spector517.xtbot.core.application.data.outbound.OutputData;

public interface Gateway<T> {

    void consume(T t) throws GatewayException;

    int produce(OutputData outputData) throws GatewayException;
}

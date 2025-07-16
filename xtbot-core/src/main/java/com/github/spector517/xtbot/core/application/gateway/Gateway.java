package com.github.spector517.xtbot.core.application.gateway;

import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.config.Config;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.data.outbound.OutputData;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.handler.EventHandler;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.properties.data.Properties;

import java.util.Map;
import java.util.Optional;

public interface Gateway {

    default Runnable consume(UpdateData updateData, Config config) throws GatewayException {
        try {
            return new EventHandler(config, this, updateData);
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    default AcceptorChecker getAcceptorChecker() {
        return AcceptorChecker.getAcceptorChecker(getProperties().version());
    }

    default ExecutorChecker getExecutorChecker() {
        return ExecutorChecker.getExecutorChecker(getProperties().version());
    }

    Optional<Integer> produce(OutputData outputData) throws GatewayException;

    Mapper<Map<String, Object>, UpdateData> getContextMapper();

    Mapper<Update, UpdateData> getApiMapper();

    Mapper<Object, Object> getActionResultMapper();

    Render getRender();

    Properties getProperties();

    AcceptorLoader getAcceptorLoader();

    ExecutorLoader getExecutorLoader();
}

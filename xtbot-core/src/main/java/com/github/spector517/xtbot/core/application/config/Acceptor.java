package com.github.spector517.xtbot.core.application.config;

import java.lang.reflect.Method;
import java.util.Map;

import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorCheckFailedException;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorNotFoundException;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.properties.AcceptorProps;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class Acceptor {

    private final Method acceptorMethod;
    private final Template valueTemplate;
    private final Mapper<Update, UpdateData> apiMapper;
    private final Mapper<Map<String, Object>, UpdateData> contextMapper;
    @Getter
    private final String name;


    Acceptor(AcceptorProps props, Gateway gateway, AcceptorChecker checker, AcceptorLoader loader) {
        this.apiMapper = gateway.getApiMapper();
        this.contextMapper = gateway.getContextMapper();
        this.valueTemplate = new Template(gateway.getRender(), props.val());
        try {
            var acceptor = loader.getAcceptor(props.acceptor());
            checker.checkAcceptor(acceptor);
            this.acceptorMethod = acceptor;
            this.name = acceptor.getAnnotation(
                com.github.spector517.xtbot.api.annotation.Acceptor.class
            ).value();
        } catch (AcceptorNotFoundException | AcceptorCheckFailedException ex) {
            throw new LoadConfigException(ex);
        }
    }

    public boolean accept(UpdateData updateData) {
        try {
            var update = apiMapper.map(updateData);
            var renderedVal = valueTemplate.value(contextMapper.map(updateData));
            return isAccepted(update, renderedVal);
        } catch(Exception e) {
            throw new AcceptorExecutionException(e);
        }
    }

    private boolean isAccepted(Update update, String renderedVal) {
        try {
            return (boolean) acceptorMethod.invoke(null, update, renderedVal);
        } catch (Exception ex) {
            return false;
        }
    }
}

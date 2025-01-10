package com.github.spector517.xtbot.core.application.config;

import java.lang.reflect.Method;
import java.util.Map;

import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorCheckFailedException;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorNotFoundException;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.properties.AcceptorProps;

public class Acceptor {

    private final Method acceptorMethod;
    private final Template valueTemplate;
    private final Mapper<UpdateData, Update> apiMapper;
    private final Mapper<UpdateData, Map<String, Object>> contextMapper;

    Acceptor(AcceptorProps props, ComponentsContainer container) {
        this.apiMapper = container.updateDataToApiMapper();
        this.contextMapper = container.updateDataToContextMapper();
        this.valueTemplate = new Template(container.render(), props.val());
        try {
            var acceptor = container.acceptorLoader().getAcceptor(props.acceptor());
            container.acceptorChecker().checkAcceptor(acceptor);
            this.acceptorMethod = acceptor;
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

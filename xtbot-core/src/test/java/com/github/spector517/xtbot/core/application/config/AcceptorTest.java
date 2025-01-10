package com.github.spector517.xtbot.core.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorCheckFailedException;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorNotFoundException;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.container.DefaultComponentsContainer;
import com.github.spector517.xtbot.core.properties.AcceptorProps;

import lombok.SneakyThrows;

class AcceptorTest {

    private DefaultComponentsContainer container;
    private AcceptorProps acceptorProps;

    private AcceptorLoader acceptorLoader;
    private AcceptorChecker acceptorChecker;
    private Mapper<UpdateData, Update> apiMapper;
    private String methodName;
    private Method method;
    private UpdateData updateData;
    private Update update;

    @SuppressWarnings("unchecked")
    @BeforeEach
    @SneakyThrows
    void setUp() {
        var acceptorValue = "val";
        update = mock(Update.class);

        method = mock(Method.class);
        methodName = "someMethod";
        when(method.invoke(null, update, acceptorValue)).thenReturn(true);

        acceptorProps = new AcceptorProps(methodName, acceptorValue);

        acceptorLoader = mock(AcceptorLoader.class);
        when(acceptorLoader.getAcceptor(methodName)).thenReturn(method);

        acceptorChecker = mock(AcceptorChecker.class);
        var render = mock(Render.class);
        updateData = mock(UpdateData.class);

        apiMapper = mock(Mapper.class);
        when(apiMapper.map(updateData)).thenReturn(update);

        Mapper<UpdateData, Map<String, Object>> contextMapper = mock(Mapper.class);

        container = mock(DefaultComponentsContainer.class);
        when(container.acceptorLoader()).thenReturn(acceptorLoader);
        when(container.acceptorChecker()).thenReturn(acceptorChecker);
        when(container.render()).thenReturn(render);
        when(container.updateDataToApiMapper()).thenReturn(apiMapper);
        when(container.updateDataToContextMapper()).thenReturn(contextMapper);
    }

    @Test
    @DisplayName("Constructor: Success create Acceptor")
    @SneakyThrows
    void constructor_0() {
        new Acceptor(acceptorProps, container);

        verify(acceptorLoader).getAcceptor(methodName);
        verify(acceptorChecker).checkAcceptor(method);
    }

    @Test
    @DisplayName("Constructor: Acceptor load failed")
    @SneakyThrows
    void constructor_1() {
        when(acceptorLoader.getAcceptor(methodName)).thenThrow(new AcceptorNotFoundException("test"));

        var ex = assertThrows(
                LoadConfigException.class,
                () -> new Acceptor(acceptorProps, container)
        );

        assertEquals(AcceptorNotFoundException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("Constructor: Acceptor check failed")
    @SneakyThrows
    void constructor_2() {
        doThrow(new AcceptorCheckFailedException("test")).when(acceptorChecker).checkAcceptor(method);

        var ex = assertThrows(
                LoadConfigException.class,
                () -> new Acceptor(acceptorProps, container)
        );

        assertEquals(AcceptorCheckFailedException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("Acceptor: accept success")
    @SneakyThrows
    void accept_0() {
        when(apiMapper.map(updateData)).thenReturn(update);

        var acceptor = new Acceptor(acceptorProps, container);
        var accepted = acceptor.accept(updateData);

        verify(method).invoke(null, update, acceptorProps.val());
        assertTrue(accepted);
    }

    @Test
    @DisplayName("Acceptor: invoke acceptor failed")
    @SneakyThrows
    void accept_1() {
        when(method.invoke(null, update, acceptorProps.val()))
                .thenThrow(new InvocationTargetException(new Exception()));

        var acceptor = new Acceptor(acceptorProps, container);
        var accepted = acceptor.accept(updateData);

        verify(method).invoke(null, update, acceptorProps.val());
        assertFalse(accepted);
    }
}
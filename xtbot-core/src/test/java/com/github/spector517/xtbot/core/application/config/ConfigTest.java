package com.github.spector517.xtbot.core.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.spector517.xtbot.api.annotation.Acceptor;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.properties.data.Properties;
import com.github.spector517.xtbot.core.properties.data.StageProps;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class ConfigTest {

    private Gateway gateway;

    private Properties properties;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        var acceptorName = "x.accept.callback";
        var executorName = "executor";

        var yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var configResPath = "properties/config.yaml";
        var props = yamlObjectMapper.readValue(
                getClass().getClassLoader().getResource(configResPath),
                Properties.class
        );
        properties = spy(props);

        Render render = mock(Render.class);
        AcceptorLoader acceptorLoader = mock(AcceptorLoader.class);
        ExecutorLoader executorLoader = mock(ExecutorLoader.class);

        var acceptor = mock(Method.class);
        var acceptorAnnotation = mock(Acceptor.class);
        var acceptorChecker = mock(AcceptorChecker.class);
        when(acceptorAnnotation.value()).thenReturn("test");
        when(acceptor.getAnnotation(Acceptor.class)).thenReturn(acceptorAnnotation);

        var executor = mock(Method.class);
        var executorAnnotation = mock(Executor.class);
        var executorChecker = mock(ExecutorChecker.class);
        when(executorAnnotation.value()).thenReturn("test");
        when(executor.getAnnotation(Executor.class)).thenReturn(executorAnnotation);
        when(executor.getReturnType()).thenReturn((Class) int.class);

        when(acceptorLoader.getAcceptor(acceptorName)).thenReturn(acceptor);
        when(executorLoader.getExecutor(executorName)).thenReturn(executor);

        gateway = mock(Gateway.class);
        when(gateway.getProperties()).thenReturn(props);
        when(gateway.getRender()).thenReturn(render);
        when(gateway.getAcceptorLoader()).thenReturn(acceptorLoader);
        when(gateway.getExecutorLoader()).thenReturn(executorLoader);
        when(gateway.getAcceptorChecker()).thenReturn(acceptorChecker);
        when(gateway.getExecutorChecker()).thenReturn(executorChecker);
        var apiDataMapper = mock(Mapper.class);
        when(gateway.getApiMapper()).thenReturn(apiDataMapper);
        var contextDataMapper = mock(Mapper.class);
        when(gateway.getContextMapper()).thenReturn(contextDataMapper);
    }

    @Test
    @DisplayName("Load config: success")
    void loadConfig_0() {
        assertDoesNotThrow(() -> new Config(gateway));
    }

    @Test
    @DisplayName("Load config: duplicates stages detected")
    void loadConfig_1() {
        var stage = properties.stages().getFirst();
        var mockStage = mock(StageProps.class);
        when(mockStage.name()).thenReturn(stage.name());
        var stagesProps = new ArrayList<>(properties.stages());
        stagesProps.add(mockStage);
        when(properties.stages()).thenReturn(stagesProps);
        when(gateway.getProperties()).thenReturn(properties);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(gateway));

        assertEquals(
                "Duplicate stage names found: %s".formatted(stage.name()),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Load config: initial stage not found")
    void loadConfig_2() {
        var stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.initial()).thenReturn(false);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);
        when(gateway.getProperties()).thenReturn(properties);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(gateway));
        assertEquals("No required initial stage found", ex.getMessage());
    }

    @Test
    @DisplayName("Load config: more one initial stages found")
    void loadConfig_3() {
        List<StageProps> stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.initial()).thenReturn(true);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);
        when(gateway.getProperties()).thenReturn(properties);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(gateway));
        assertEquals(
                "Only one initial stage allowed, but found: %s".formatted(
                    String.join(", ", stagesProps.stream().map(StageProps::name).toList())
                ),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Load config: fail stage not found")
    void loadConfig_4() {
        var stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.fail()).thenReturn(false);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);
        when(gateway.getProperties()).thenReturn(properties);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(gateway));
        assertEquals("No required fail stage found", ex.getMessage());
    }

    @Test
    @DisplayName("Load config: more one fail stages found")
    void loadConfig_5() {
        List<StageProps> stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.fail()).thenReturn(true);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);
        when(gateway.getProperties()).thenReturn(properties);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(gateway));
        assertEquals(
                "Only one fail stage allowed, but found: %s".formatted(
                    String.join(", ", stagesProps.stream().map(StageProps::name).toList())
                ),
                ex.getMessage()
        );
    }
}
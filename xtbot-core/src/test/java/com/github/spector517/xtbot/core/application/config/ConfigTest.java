package com.github.spector517.xtbot.core.application.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorChecker;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.container.DefaultComponentsContainer;
import com.github.spector517.xtbot.core.properties.LoadPropertiesException;
import com.github.spector517.xtbot.core.properties.Properties;
import com.github.spector517.xtbot.core.properties.PropertiesLoader;
import com.github.spector517.xtbot.core.properties.StageProps;

import lombok.SneakyThrows;

class ConfigTest {

    private DefaultComponentsContainer container;

    private PropertiesLoader propertiesLoader;
    private Render render;
    private AcceptorLoader acceptorLoader;
    private AcceptorChecker acceptorChecker;
    private ExecutorLoader executorLoader;
    private ExecutorChecker executorChecker;
    private Mapper<UpdateData, Update> apiDataMapper;
    private Mapper<UpdateData, Map<String, Object>> contextDataMapper;
    private ObjectMapper jsonObjectMapper;
    private Properties properties;
    private String acceptorName;
    private String executorName;
    private Method acceptor;
    private Method executor;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        acceptorName = "callback";
        executorName = "executor";

        var yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var configResPath = "properties/config.yaml";
        var props = yamlObjectMapper.readValue(
                getClass().getClassLoader().getResource(configResPath),
                Properties.class
        );
        properties = spy(props);

        propertiesLoader = mock(PropertiesLoader.class);
        when(propertiesLoader.load()).thenReturn(properties);

        render = mock(Render.class);
        acceptorLoader = mock(AcceptorLoader.class);
        executorLoader = mock(ExecutorLoader.class);
        acceptor = mock(Method.class);
        acceptorChecker = mock(AcceptorChecker.class);
        executor = mock(Method.class);
        executorChecker = mock(ExecutorChecker.class);
        jsonObjectMapper = new ObjectMapper();
        when(acceptorLoader.getAcceptor(acceptorName)).thenReturn(acceptor);
        when(executorLoader.getExecutor(executorName)).thenReturn(executor);

        container = mock(DefaultComponentsContainer.class);
        when(container.propertiesLoader()).thenReturn(propertiesLoader);
        when(container.render()).thenReturn(render);
        when(container.acceptorLoader()).thenReturn(acceptorLoader);
        when(container.executorLoader()).thenReturn(executorLoader);
        when(container.acceptorChecker()).thenReturn(acceptorChecker);
        when(container.executorChecker()).thenReturn(executorChecker);
        when(container.updateDataToApiMapper()).thenReturn(apiDataMapper);
        when(container.updateDataToContextMapper()).thenReturn(contextDataMapper);
        when(container.jsonObjectMapper()).thenReturn(jsonObjectMapper);
        when(container.yamlObjectMapper()).thenReturn(yamlObjectMapper);
    }

    @Test
    @DisplayName("Load config: success")
    void loadConfig_0() {
        assertDoesNotThrow(() -> new Config(container));
    }

    @Test
    @DisplayName("Load config: error while loading properties")
    @SneakyThrows
    void loadConfig_1() {
        when(propertiesLoader.load()).thenThrow(new LoadPropertiesException("test"));
        var ex = assertThrows(LoadConfigException.class, () -> new Config(container));
        assertEquals(LoadPropertiesException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("Load config: duplicates stages detected")
    void loadConfig_2() {
        var stage = properties.stages().getFirst();
        var mockStage = mock(StageProps.class);
        when(mockStage.name()).thenReturn(stage.name());
        var stagesProps = new ArrayList<>(properties.stages());
        stagesProps.add(mockStage);
        when(properties.stages()).thenReturn(stagesProps);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(container));

        assertEquals(
                "Duplicate stage names found: %s".formatted(stage.name()),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Load config: initial stage not found")
    void loadConfig_3() {
        var stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.initial()).thenReturn(false);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(container));
        assertEquals("No required initial stage found", ex.getMessage());
    }

    @Test
    @DisplayName("Load config: more one initial stages found")
    void loadConfig_4() {
        List<StageProps> stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.initial()).thenReturn(true);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(container));
        assertEquals(
                "Only one initial stage allowed, but found: %s".formatted(
                    String.join(", ", stagesProps.stream().map(StageProps::name).toList())
                ),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Load config: fail stage not found")
    void loadConfig_5() {
        var stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.fail()).thenReturn(false);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(container));
        assertEquals("No required fail stage found", ex.getMessage());
    }

    @Test
    @DisplayName("Load config: more one fail stages found")
    void loadConfig_6() {
        List<StageProps> stagesProps = properties.stages().stream().map(stageProps -> {
            var spyStage = spy(stageProps);
            when(spyStage.fail()).thenReturn(true);
            return spyStage;
        }).toList();
        when(properties.stages()).thenReturn(stagesProps);

        var ex = assertThrows(LoadConfigException.class, () -> new Config(container));
        assertEquals(
                "Only one fail stage allowed, but found: %s".formatted(
                    String.join(", ", stagesProps.stream().map(StageProps::name).toList())
                ),
                ex.getMessage()
        );
    }
}
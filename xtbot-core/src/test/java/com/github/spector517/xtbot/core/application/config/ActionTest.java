package com.github.spector517.xtbot.core.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorCheckFailedException;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorNotFoundException;
import com.github.spector517.xtbot.core.application.mapper.Mapper;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.container.DefaultComponentsContainer;
import com.github.spector517.xtbot.core.properties.ActionProps;

import lombok.SneakyThrows;

@SuppressWarnings({"unchecked", "rawtypes"})
class ActionTest {

    private DefaultComponentsContainer container;
    private ActionProps actionProps;

    private ExecutorLoader executorLoader;
    private ExecutorChecker executorChecker;
    private ObjectMapper objectMapper;
    private String methodName;
    private Render render;
    private UpdateData updateData;
    private Method method;
    private Map<String, Object> args;
    private String resultVarName;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        methodName = "method";
        var paramName1 = "arg1";
        var paramName2 = "arg2";
        args = Map.of(paramName2, "value2", paramName1, 1);
        method = mock(Method.class);
        resultVarName = "var_name";
        var param1 = mock(Parameter.class);
        var param2 = mock(Parameter.class);
        when(param1.getName()).thenReturn(paramName1);
        when(param2.getName()).thenReturn(paramName2);
        when(method.getParameters()).thenReturn(new Parameter[]{param1, param2});

        actionProps = new ActionProps(methodName, args, resultVarName);

        executorLoader = mock(ExecutorLoader.class);
        when(executorLoader.getExecutor(methodName)).thenReturn(method);

        executorChecker = mock(ExecutorChecker.class);
        objectMapper = mock(ObjectMapper.class);
        render = mock(Render.class);
        updateData = mock(UpdateData.class);

        Mapper<UpdateData, Map<String, Object>> contextMapper = mock(Mapper.class);

        container = mock(DefaultComponentsContainer.class);
        when(container.executorLoader()).thenReturn(executorLoader);
        when(container.executorChecker()).thenReturn(executorChecker);
        when(container.jsonObjectMapper()).thenReturn(objectMapper);
        when(container.render()).thenReturn(render);
        when(container.updateDataToContextMapper()).thenReturn(contextMapper);
    }

    @Test
    @DisplayName("Constructor: Success create Action")
    @SneakyThrows
    void constructor_1() {
        new Action(actionProps, container);

        verify(executorLoader).getExecutor(methodName);
        verify(executorChecker).checkExecutor(method, args);

    }

    @Test
    @DisplayName("Constructor: Executor load is failed")
    @SneakyThrows
    void constructor_2() {
        when(executorLoader.getExecutor(methodName)).thenThrow(ExecutorNotFoundException.class);

        var ex = assertThrows(
            LoadConfigException.class,
            () -> new Action(actionProps, container)
        );

        assertEquals(ExecutorNotFoundException.class, ex.getCause().getClass());
        verify(executorChecker, never()).checkExecutor(method, args);
    }

    @Test
    @DisplayName("Constructor: Executor check is failed")
    @SneakyThrows
    void constructor_3() {
        doThrow(ExecutorCheckFailedException.class).when(executorChecker)
            .checkExecutor(method, args);

        var ex = assertThrows(
            LoadConfigException.class,
            () -> new Action(actionProps, container)
        );

        assertEquals(ExecutorCheckFailedException.class, ex.getCause().getClass());
        verify(executorChecker).checkExecutor(method, args);
    }

    @Test
    @DisplayName("Execute: simple return value")
    @SneakyThrows
    void execute_1() {
        var result = "result";
        when(method.getReturnType()).thenReturn((Class) String.class);
        when(method.invoke(null, 1, "value2")).thenReturn(result);

        var action = new Action(actionProps, container);
        var actualResult = action.execute(updateData);

        assertEquals(result, actualResult);
        verify(objectMapper, never()).convertValue(any(), any(Class.class));
    }

    @Test
    @DisplayName("Execute: mapping return value")
    @SneakyThrows
    void execute_2() {
        var result = Map.of("key", "value");
        when(method.getReturnType()).thenReturn((Class) getClass());
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(result);
        when(method.invoke(null, 1, "value2")).thenReturn(result);

        var action = new Action(actionProps, container);
        var actualResult = action.execute(updateData);

        assertEquals(result, actualResult);
        verify(objectMapper).convertValue(result, Map.class);
    }

    @Test
    @DisplayName("Execute: invoke executor failed")
    @SneakyThrows
    void execute_3() {
        when(method.getReturnType()).thenReturn((Class) getClass());
        when(method.invoke(null, 1, "value2"))
            .thenThrow(new InvocationTargetException(new Exception()));

        var action = new Action(actionProps, container);
        var ex = assertThrows(ActionExecutionException.class, () -> action.execute(updateData));

        assertEquals(ex.getCause().getClass(), InvocationTargetException.class);
    }

    @Test
    @DisplayName("Execute: templates mixed args")
    @SneakyThrows
    void execute_4() {
        var paramName1 = "stringArg";
        var paramName2 = "listArg";
        var paramName3 = "mapArg";
        var param1 = mock(Parameter.class);
        var param2 = mock(Parameter.class);
        var param3 = mock(Parameter.class);
        when(param1.getName()).thenReturn(paramName1);
        when(param2.getName()).thenReturn(paramName2);
        when(param3.getName()).thenReturn(paramName3);
        when(method.getParameters()).thenReturn(new Parameter[]{param1, param2, param3});
        when(method.getReturnType()).thenReturn((Class) long.class);
        var result = 777L;
        var stringArg = "{{ template0 }}";
        var listArg = List.of("{{ template1 }}", "{{ template2 }}");
        var mapArg = Map.of("{{ template3 }}", "{{ template4 }}");
        var arguments = Map.of(
            "listArg", listArg,
            "mapArg", mapArg,
            "stringArg", stringArg
        );
        when(method.invoke(
            null, 
            "render0", 
            List.of("render1", "render2"),
            Map.of("render3", "render4")
        )).thenReturn(result);
        when(render.isTemplate(anyString())).thenReturn(true);
        when(render.render(eq("{{ template0 }}"), any())).thenReturn("render0");
        when(render.render(eq("{{ template1 }}"), any())).thenReturn("render1");
        when(render.render(eq("{{ template2 }}"), any())).thenReturn("render2");
        when(render.render(eq("{{ template3 }}"), any())).thenReturn("render3");
        when(render.render(eq("{{ template4 }}"), any())).thenReturn("render4");
        when(render.render(eq("mapArg"), any())).thenReturn("mapArg");
        when(render.render(eq("stringArg"), any())).thenReturn("stringArg");
        when(render.render(eq("listArg"), any())).thenReturn("listArg");

        var action = new Action(new ActionProps(methodName, arguments, resultVarName), container);
        var actualResult = action.execute(updateData);

        assertEquals(result, actualResult);
        verify(method).invoke(
            null,
            "render0",
            List.of("render1", "render2"),
            Map.of("render3", "render4")
        );
    }
}

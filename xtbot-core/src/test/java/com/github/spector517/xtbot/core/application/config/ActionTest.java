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

import com.github.spector517.xtbot.core.application.gateway.Gateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.core.application.data.inbound.UpdateData;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorCheckFailedException;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorNotFoundException;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.properties.ActionProps;

import lombok.SneakyThrows;
import org.mockito.ArgumentCaptor;

@SuppressWarnings({"unchecked", "rawtypes"})
class ActionTest {

    private Gateway gateway;
    private ActionProps actionProps;

    private ExecutorLoader executorLoader;
    private ExecutorChecker executorChecker;
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
        var annotation = mock(Executor.class);
        when(annotation.value()).thenReturn("test");
        when(param1.getName()).thenReturn(paramName1);
        when(param2.getName()).thenReturn(paramName2);
        when(method.getParameters()).thenReturn(new Parameter[]{param1, param2});
        when(method.getAnnotation(Executor.class)).thenReturn(annotation);

        actionProps = new ActionProps(methodName, args, resultVarName);

        executorLoader = mock(ExecutorLoader.class);
        when(executorLoader.getExecutor(methodName)).thenReturn(method);

        executorChecker = mock(ExecutorChecker.class);
        render = mock(Render.class);
        updateData = mock(UpdateData.class);

        Mapper<Map<String, Object>, UpdateData> contextMapper = mock(Mapper.class);
        Mapper<Object, Object> actionResultMapper = mock(Mapper.class);

        gateway = mock(Gateway.class);
        when(gateway.getRender()).thenReturn(render);
        when(gateway.getContextMapper()).thenReturn(contextMapper);
        when(gateway.getActionResultMapper()).thenReturn(actionResultMapper);
    }

    @Test
    @DisplayName("Constructor: Success create Action")
    @SneakyThrows
    void constructor_1() {
        new Action(actionProps, gateway, executorChecker, executorLoader);

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
            () -> new Action(actionProps, gateway, executorChecker, executorLoader)
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
            () -> new Action(actionProps, gateway, executorChecker, executorLoader)
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

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var actualResult = action.execute(updateData);

        assertEquals(result, actualResult);
        verify(gateway.getActionResultMapper(), never()).map(any(), any(Class.class));
    }

    @Test
    @DisplayName("Execute: mapping return value")
    @SneakyThrows
    void execute_2() {
        var result = Map.of("key", "value");
        when(method.getReturnType()).thenReturn((Class) getClass());
        when(gateway.getActionResultMapper().map(result, Map.class)).thenReturn(result);
        when(method.invoke(null, 1, "value2")).thenReturn(result);

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var actualResult = action.execute(updateData);

        var objectCaptor = ArgumentCaptor.forClass(Object.class);
        verify(gateway.getActionResultMapper()).map(objectCaptor.capture(), eq(Map.class));
        assertEquals(result, objectCaptor.getValue());
        assertEquals(result, actualResult);
    }

    @Test
    @DisplayName("Execute: listable return value")
    @SneakyThrows
    void execute_3() {
        var result = List.of("value1", "value2");
        when(method.getReturnType()).thenReturn((Class) Object[].class);
        when(gateway.getActionResultMapper().map(result, List.class)).thenReturn(result);
        when(method.invoke(null, 1, "value2")).thenReturn(result);

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var actualResult = action.execute(updateData);

        var objectCaptor = ArgumentCaptor.forClass(Object.class);
        verify(gateway.getActionResultMapper()).map(objectCaptor.capture(), eq(List.class));
        assertEquals(result, objectCaptor.getValue());
        assertEquals(result, actualResult);
    }

    @Test
    @DisplayName("Execute: invoke executor failed")
    @SneakyThrows
    void execute_4() {
        when(method.getReturnType()).thenReturn((Class) getClass());
        when(method.invoke(null, 1, "value2"))
            .thenThrow(new InvocationTargetException(new Exception()));

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var ex = assertThrows(ActionExecutionException.class, () -> action.execute(updateData));

        assertEquals(InvocationTargetException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("Execute: templates mixed args")
    @SneakyThrows
    void execute_5() {
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

        var action = new Action(
                new ActionProps(methodName, arguments, resultVarName), gateway, executorChecker, executorLoader
        );
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

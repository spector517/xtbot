package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.api.annotation.Default;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorCheckFailedException;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorChecker;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorLoader;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorNotFoundException;
import com.github.spector517.xtbot.core.application.gateway.Gateway;
import com.github.spector517.xtbot.core.application.render.Render;
import com.github.spector517.xtbot.core.mapper.Mapper;
import com.github.spector517.xtbot.core.properties.data.ActionProps;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class ActionTest {

    private Gateway gateway;
    private ActionProps actionProps;

    private ExecutorLoader executorLoader;
    private ExecutorChecker executorChecker;
    private String methodName;
    private Render render;
    private Map<String, Object> context;
    private Method method;
    private Map<String, Object> args;
    private String resultVarName;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        methodName = "method";
        var paramName1 = "arg1";
        var paramName2 = "arg2";
        var paramName3 = "arg3";
        args = Map.of(paramName2, "value2", paramName1, 1);
        method = mock(Method.class);
        resultVarName = "var_name";
        var param1 = mock(Parameter.class);
        var param2 = mock(Parameter.class);
        var param3 = mock(Parameter.class);

        var componentAnnotation = mock(Executor.class);
        when(componentAnnotation.value()).thenReturn("test");
        var param2DefaultAnnotation = mock(Default.class);
        when(param2DefaultAnnotation.type()).thenReturn(Default.Type.STRING);
        when(param2DefaultAnnotation.value()).thenReturn("defaultValue2");
        var param3DefaultAnnotation = mock(Default.class);
        when(param3DefaultAnnotation.type()).thenReturn(Default.Type.DOUBLE);
        when(param3DefaultAnnotation.doubleValue()).thenReturn(2.04);

        when(param1.getName()).thenReturn(paramName1);
        when(param2.getName()).thenReturn(paramName2);
        when(param2.isAnnotationPresent(Default.class)).thenReturn(true);
        when(param2.getAnnotation(Default.class)).thenReturn(param2DefaultAnnotation);
        when(param3.getName()).thenReturn(paramName3);
        when(param3.isAnnotationPresent(Default.class)).thenReturn(true);
        when(param3.getAnnotation(Default.class)).thenReturn(param3DefaultAnnotation);
        when(method.getParameters()).thenReturn(new Parameter[]{param1, param2, param3});
        when(method.getAnnotation(Executor.class)).thenReturn(componentAnnotation);

        actionProps = new ActionProps(methodName, args, resultVarName);

        executorLoader = mock(ExecutorLoader.class);
        when(executorLoader.getExecutor(methodName)).thenReturn(method);

        executorChecker = mock(ExecutorChecker.class);
        render = mock(Render.class);
        context = Map.of();

        Mapper<Object, Object> actionResultMapper = mock(Mapper.class);

        gateway = mock(Gateway.class);
        when(gateway.getRender()).thenReturn(render);
        when(gateway.getActionResultMapper()).thenReturn(actionResultMapper);
    }

    @Test
    @DisplayName("Constructor: Success create Action")
    @SneakyThrows
    void constructor_1() {
        when(method.getReturnType()).thenReturn((Class) String.class);
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
        when(method.invoke(null, 1, "value2", 2.04)).thenReturn(result);

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var actualResult = action.execute(context);

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
        when(method.invoke(null, 1, "value2", 2.04)).thenReturn(result);

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var actualResult = action.execute(context);

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
        when(method.invoke(null, 1, "value2", 2.04)).thenReturn(result);

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var actualResult = action.execute(context);

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
        when(method.invoke(null, 1, "value2", 2.04))
            .thenThrow(new InvocationTargetException(new Exception()));

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var ex = assertThrows(ActionExecutionException.class, () -> action.execute(context));

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
        var actualResult = action.execute(context);

        assertEquals(result, actualResult);
        verify(method).invoke(
            null,
            "render0",
            List.of("render1", "render2"),
            Map.of("render3", "render4")
        );
    }

    @Test
    @DisplayName("Execute: no args")
    @SneakyThrows
    void execute_6() {
        var expectedResult = "result";
        when(method.getParameters()).thenReturn(new Parameter[0]);
        when(method.getReturnType()).thenReturn((Class) String.class);
        when(method.invoke(null)).thenReturn(expectedResult);

        var action = new Action(
                new ActionProps(methodName, null, resultVarName),gateway, executorChecker, executorLoader
        );
        var actualResult = action.execute(context);

        assertEquals(expectedResult, actualResult);
        verify(method).invoke(null);
    }

    @Test
    @DisplayName("Execute: with default args")
    @SneakyThrows
    void execute_7() {
        args = Map.of("arg1", 1);
        actionProps = new ActionProps(methodName, args, resultVarName);
        var result = "result";
        when(method.getReturnType()).thenReturn((Class) String.class);
        when(method.invoke(null, 1, "defaultValue2", 2.04)).thenReturn(result);

        var action = new Action(actionProps, gateway, executorChecker, executorLoader);
        var actualResult = action.execute(context);

        verify(method).invoke(null, 1, "defaultValue2", 2.04);
        verify(gateway.getActionResultMapper(), never()).map(any(), any(Class.class));
        assertEquals(result, actualResult);
    }
}

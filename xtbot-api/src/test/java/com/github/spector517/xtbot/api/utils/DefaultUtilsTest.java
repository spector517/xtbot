package com.github.spector517.xtbot.api.utils;

import com.github.spector517.xtbot.api.annotation.Default;
import com.github.spector517.xtbot.api.utils.exception.PredefinedAnnotationNotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unused"})
class DefaultUtilsTest {

    private Parameter predefinedParameter;

    @BeforeEach
    void setUp() {
        predefinedParameter = mock(Parameter.class);
        when(predefinedParameter.getName()).thenReturn("var777");
    }

    @Test
    @DisplayName("No annotation present")
    void getDefaultValue_0() {
        when(predefinedParameter.isAnnotationPresent(Default.class)).thenReturn(false);

        var ex = assertThrows(
                PredefinedAnnotationNotFoundException.class,
                () -> DefaultUtils.getDefaultValue(predefinedParameter)
        );
        assertEquals(
                "Parameter var777 has no @Default annotation",
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Fixed value types")
    void getDefaultValue_1() {
        var annotation = mock(Default.class);
        when(predefinedParameter.isAnnotationPresent(Default.class)).thenReturn(true);
        when(predefinedParameter.getAnnotation(Default.class)).thenReturn(annotation);

        when(annotation.type()).thenReturn(Default.Type.BOOLEAN);
        when(annotation.boolValue()).thenReturn(true);
        assertTrue((Boolean) DefaultUtils.getDefaultValue(predefinedParameter));

        when(annotation.type()).thenReturn(Default.Type.INTEGER);
        when(annotation.intValue()).thenReturn(32);
        assertEquals(
                32,
                DefaultUtils.getDefaultValue(predefinedParameter)
        );

        when(annotation.type()).thenReturn(Default.Type.DOUBLE);
        when(annotation.doubleValue()).thenReturn(2.04);
        assertEquals(
                2.04,
                DefaultUtils.getDefaultValue(predefinedParameter)
        );

        when(annotation.type()).thenReturn(Default.Type.STRING);
        when(annotation.value()).thenReturn("strValue");
        assertEquals(
                "strValue",
                DefaultUtils.getDefaultValue(predefinedParameter)
        );
    }

    @Test
    @DisplayName("Autodetect value type")
    void getDefaultValue_2() {
        var parameter = getParameterByMethod("testBoolean", boolean.class);
        assertTrue((Boolean) DefaultUtils.getDefaultValue(parameter));

        parameter = getParameterByMethod("testInteger", int.class);
        assertEquals(32, DefaultUtils.getDefaultValue(parameter));

        parameter = getParameterByMethod("testInteger2", Integer.class);
        assertEquals(33, DefaultUtils.getDefaultValue(parameter));

        parameter = getParameterByMethod("testDouble", double.class);
        assertEquals(2.04, DefaultUtils.getDefaultValue(parameter));

        parameter = getParameterByMethod("testDouble2", double.class);
        assertEquals(10d, DefaultUtils.getDefaultValue(parameter));

        parameter = getParameterByMethod("testStr", String.class);
        assertEquals("test", DefaultUtils.getDefaultValue(parameter));
    }

    @SneakyThrows
    Parameter getParameterByMethod(String methodName, Class<?>... types) {
        return this.getClass().getMethod(methodName, types).getParameters()[0];
    }

    public static void testBoolean(
            @Default(boolValue = true) boolean b
    ) {
        throw new UnsupportedOperationException("Test method call");
    }

    public static void testInteger(
            @Default(intValue = 32) int i
    ) {
        throw new UnsupportedOperationException("Test method call");
    }

    public static void testInteger2(
            @Default(intValue = 33) Integer i
    ) {
        throw new UnsupportedOperationException("Test method call");
    }

    public static void testDouble(
            @Default(doubleValue = 2.04) double d
    ) {
        throw new UnsupportedOperationException("Test method call");
    }

    public static void testDouble2(
            @Default(doubleValue = 10) double d
    ) {
        throw new UnsupportedOperationException("Test method call");
    }

    public static void testStr(
            @Default("test") String str
    ) {
        throw new UnsupportedOperationException("Test method call");
    }

}
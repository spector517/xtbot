package com.github.spector517.xtbot.core.application.extension.acceptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.spector517.xtbot.api.dto.Update;

@SuppressWarnings({ "unchecked", "rawtypes" })
class AcceptorCheckerV1Test {

    private Method acceptorMock;
    private String acceptorMockName;
    private Parameter updateParameterMock;
    private Parameter valParameterMock;
    private Parameter invalidParameterMock;


    @BeforeEach
    void setUp() {
        acceptorMock = Mockito.mock(Method.class);
        acceptorMockName = "test";
        updateParameterMock = Mockito.mock(Parameter.class);
        valParameterMock = Mockito.mock(Parameter.class);
        invalidParameterMock = Mockito.mock(Parameter.class);
        when(updateParameterMock.getType()).thenReturn((Class) Update.class);
        when(valParameterMock.getType()).thenReturn((Class) String.class);
        when(invalidParameterMock.getType()).thenReturn((Class) this.getClass());
        when(acceptorMock.getName()).thenReturn(acceptorMockName);
        when(acceptorMock.getReturnType()).thenReturn((Class) boolean.class);
        when(acceptorMock.getParameters()).thenReturn(new Parameter[]{updateParameterMock, valParameterMock});
    }

    @Test
    @DisplayName("Success: Valid method")
    void checkAcceptor_0() {
        var checker = new AcceptorCheckerV1();
        assertDoesNotThrow(() -> checker.checkAcceptor(acceptorMock));
    }

    @Test
    @DisplayName("Fail: Invalid parameters count")
    void checkAcceptor_invalidParameterCount_throwsException() {
        when(acceptorMock.getParameters()).thenReturn(new Parameter[]{updateParameterMock});
        var checker = new AcceptorCheckerV1();
        var ex = assertThrows(
            AcceptorCheckFailedException.class,
            () -> checker.checkAcceptor(acceptorMock)
        );
        assertEquals(
                "Acceptor method must have 2 parameters: [%s], [%s], but got 1: [%s]".formatted(
                        Update.class, String.class, Update.class
                ),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Fail: Invalid parameter type")
    void checkAcceptor_invalidParameterType_throwsException() {
        when(acceptorMock.getParameters()).thenReturn(new Parameter[]{invalidParameterMock, valParameterMock});
        AcceptorCheckerV1 checker = new AcceptorCheckerV1();
        var ex = assertThrows(
            AcceptorCheckFailedException.class,
            () -> checker.checkAcceptor(acceptorMock)
        );
        assertEquals(
                "Acceptor method must have 2 parameters: [%s], [%s], but got 2: [%s], [%s]".formatted(
                        Update.class, String.class, this.getClass(), String.class
                ),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Fail: Invalid return type")
    void checkAcceptor_invalidReturnType_throwsException() {
        when(acceptorMock.getReturnType()).thenReturn((Class) int.class);
        AcceptorCheckerV1 checker = new AcceptorCheckerV1();
        var ex = assertThrows(
            AcceptorCheckFailedException.class,
            () -> checker.checkAcceptor(acceptorMock)
        );
        assertEquals(
                "Return type of Acceptor method '%s' must be a boolean, not [%s]"
                        .formatted(acceptorMockName, int.class),
                ex.getMessage()
        );
    }
}


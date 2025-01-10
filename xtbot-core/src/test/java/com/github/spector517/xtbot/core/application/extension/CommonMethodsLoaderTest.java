package com.github.spector517.xtbot.core.application.extension;

import com.github.spector517.xtbot.api.dto.Update;
import com.github.spector517.xtbot.core.application.extension.acceptor.AcceptorNotFoundException;
import com.github.spector517.xtbot.core.application.extension.executor.ExecutorNotFoundException;
import com.github.spector517.xtbot.core.application.loader.BotClassLoader;
import com.github.spector517.xtbot.core.common.TestComponent;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonMethodsLoaderTest {

    @Mock
    private BotClassLoader botClassLoader;

    @BeforeEach
    void setUp() {
        when(botClassLoader.getBotComponents()).thenReturn(Set.of(TestComponent.class));
    }

    @Test
    @DisplayName("Get acceptor: success")
    @SneakyThrows
    void getAcceptor_0() {
        var commonMethodsLoader = new CommonMethodsLoader(botClassLoader);
        var expectedAcceptor = TestComponent.class.getDeclaredMethod("accept", Update.class, String.class);
        var actualAcceptor = commonMethodsLoader.getAcceptor("acc1");
        assertEquals(expectedAcceptor, actualAcceptor);
    }

    @Test
    @DisplayName("Get acceptor: acceptor not found")
    void getAcceptor_1() {
        var commonMethodsLoader = new CommonMethodsLoader(botClassLoader);
        assertThrows(
                AcceptorNotFoundException.class,
                () -> commonMethodsLoader.getAcceptor("wrongAcceptor")
        );
    }

    @Test
    @DisplayName("Get executor: success")
    @SneakyThrows
    void getExecutor_0() {
        var commonMethodsLoader = new CommonMethodsLoader(botClassLoader);
        var expectedExecutor = TestComponent.class.getDeclaredMethod("execute", String.class, Integer.class);
        var actualExecutor = commonMethodsLoader.getExecutor("exec1");
        assertEquals(expectedExecutor, actualExecutor);
    }

    @Test
    @DisplayName("Get executor: executor not found")
    void getExecutor_1() {
        var commonMethodsLoader = new CommonMethodsLoader(botClassLoader);
        assertThrows(
                ExecutorNotFoundException.class,
                () -> commonMethodsLoader.getExecutor("wrongExecutor")
        );
    }
}
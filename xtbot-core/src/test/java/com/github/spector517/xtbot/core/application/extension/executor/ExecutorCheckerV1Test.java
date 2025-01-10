package com.github.spector517.xtbot.core.application.extension.executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.spector517.xtbot.core.common.TestComponent;

import lombok.SneakyThrows;

class ExecutorCheckerV1Test {

    private Map<String, Object> simpleArgs;
    private Map<String, Object> primitiveArgs;
    private Map<String, Object> containerArgs;
    private Map<String, Object> mixedArgs;
    private Class<?> testComponent;
    private String testComponentMethodName;

    @BeforeEach
    void setUp() {
        simpleArgs = new HashMap<>(Map.of(
                "name", "Alex",
                "age", 25
        ));
        primitiveArgs = new HashMap<>(Map.of(
                "radius", 5,
                "strict", true
        ));
        containerArgs = new HashMap<>(Map.of(
                "users", Arrays.asList("Alex", "Brian"),
                "data", Map.of(
                        "age", 25,
                        "isMale", true
                )
        ));
        mixedArgs = new HashMap<>(Map.of(
                "name", "Alex",
                "age", 25,
                "isMale", true,
                "cars", List.of("BMW", "Ford", "Toyota"),
                "data", Map.of("someKey", "someValue",
                        "anotherKey", false
                )
        ));
        testComponent = TestComponent.class;
        testComponentMethodName = "execute";
    }

    @Test
    @DisplayName("Simple arguments: success")
    void checkExecutor_0() {
        var method = getTestExecutor(String.class, Integer.class);
        var checker = new ExecutorCheckerV1();
        assertDoesNotThrow(() -> checker.checkExecutor(method, simpleArgs));
    }

    @Test
    @DisplayName("Simple arguments: missing required arguments")
    void checkExecutor_1() {
        var method = getTestExecutor(String.class, Integer.class);
        var checker = new ExecutorCheckerV1();
        var ex = assertThrows(
                ExecutorCheckFailedException.class,
                () -> checker.checkExecutor(method, Map.of("name", "Alex"))
        );
        assertEquals("Missing arguments: age", ex.getMessage());
    }

    @Test
    @DisplayName("Primitive arguments: success")
    void checkExecutor_2() {
        var method = getTestExecutor(int.class, boolean.class);
        var checker = new ExecutorCheckerV1();
        assertDoesNotThrow(() -> checker.checkExecutor(method, primitiveArgs));
    }

    @Test
    @DisplayName("Primitive arguments: Unknown arguments")
    void checkExecutor_3() {
        var method = getTestExecutor(int.class, boolean.class);
        var checker = new ExecutorCheckerV1();
        primitiveArgs.put("extra1", "extra");
        var ex = assertThrows(
                ExecutorCheckFailedException.class,
                () -> checker.checkExecutor(method, primitiveArgs)
        );
        assertEquals("Unknown arguments: extra1", ex.getMessage());
    }

    @Test
    @DisplayName("Container arguments: success")
    void checkExecutor_4() {
        var method = getTestExecutor(List.class, Map.class);
        var checker = new ExecutorCheckerV1();
        assertDoesNotThrow(() -> checker.checkExecutor(method, containerArgs));
    }

    @Test
    @DisplayName("Container arguments: missing and unknown")
    void checkExecutor_5() {
        var method = getTestExecutor(List.class, Map.class);
        var checker = new ExecutorCheckerV1();
        var unknownArgumentName = "extra";
        var removedArgumentName = "users";
        containerArgs.remove(removedArgumentName);
        containerArgs.put("extra", List.of());
        var ex = assertThrows(
            ExecutorCheckFailedException.class,
            () -> checker.checkExecutor(method, containerArgs)
        );
        assertEquals(
                "Missing arguments: %s, but got: %s".formatted(removedArgumentName, unknownArgumentName),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Mixed arguments: success")
    void checkExecutor_6() {
        var method = getTestExecutor(
            String.class, Integer.class, boolean.class, List.class, Map.class
        );
        var checker = new ExecutorCheckerV1();
        assertDoesNotThrow(() -> checker.checkExecutor(method, mixedArgs));
    }

    @Test
    @DisplayName("Mixed arguments: unexpected implementation")
    void checkExecutor_7() {
        var method = getTestExecutor(
            String.class, Integer.class, boolean.class, List.class, Map.class
        );
        var checker = new ExecutorCheckerV1();
        mixedArgs.put("cars", "BWW, Ford, Toyota");
        var ex = assertThrows(
                ExecutorCheckFailedException.class,
                () -> checker.checkExecutor(method, mixedArgs)
        );
        assertEquals(
                "For argument 'cars' expected implementation of '%s' but got '%s'".formatted(
                    List.class, String.class
                ),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Mixed arguments: unexpected type")
    void checkExecutor_8() {
        var method = getTestExecutor(
            String.class, Integer.class, boolean.class, List.class, Map.class
        );
        var checker = new ExecutorCheckerV1();
        mixedArgs.put("age", "25");
        var ex = assertThrows(
                ExecutorCheckFailedException.class,
                () -> checker.checkExecutor(method, mixedArgs)
        );
        assertEquals(
                "Argument 'age' has type '%s' but expected '%s".formatted(String.class, Integer.class),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Simple arguments: private modifier")
    void checkExecutor_9() {
        var method = getTestExecutor(String.class);
        var checker = new ExecutorCheckerV1();
        var ex = assertThrows(
                ExecutorCheckFailedException.class,
                () -> checker.checkExecutor(method, Map.of("test", "test"))
        );
        assertEquals(
                "Executor method '%s' must be public".formatted(testComponentMethodName),
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("Simple arguments: non-static method")
    void checkExecutor_10() {
        var method = getTestExecutor(Integer.class);
        var checker = new ExecutorCheckerV1();
        var ex = assertThrows(
                ExecutorCheckFailedException.class,
                () -> checker.checkExecutor(method, Map.of("test", Boolean.TRUE))
        );
        assertEquals(
                "Executor method '%s' must be static".formatted(testComponentMethodName),
                ex.getMessage()
        );
    }
    

    @SneakyThrows
    private Method getTestExecutor(Class<?>... types) {
        try {
            return testComponent.getMethod(testComponentMethodName, types);
        } catch (NoSuchMethodException e) {
            return testComponent.getDeclaredMethod(testComponentMethodName, types);
        }
    }
}
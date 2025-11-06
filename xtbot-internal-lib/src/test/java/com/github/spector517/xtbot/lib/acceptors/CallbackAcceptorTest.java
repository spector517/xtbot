package com.github.spector517.xtbot.lib.acceptors;

import com.github.spector517.xtbot.api.dto.Callback;
import com.github.spector517.xtbot.api.dto.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CallbackAcceptorTest {

    private Update update;
    private Callback callback;

    @BeforeEach
    void setUp() {
        update = mock(Update.class);
        callback = mock(Callback.class);
    }

    @Test
    @DisplayName("Callback exists and data is correct")
    void testIsAccepted_0() {
        when(update.callback()).thenReturn(callback);
        when(callback.data()).thenReturn("test");

        assertTrue(CallbackAcceptor.isAccepted(update, "test"));
    }

    @Test
    @DisplayName("Callback exists and data is invalid")
    void testIsAccepted_1() {
        when(update.callback()).thenReturn(callback);
        when(callback.data()).thenReturn("test");

        assertFalse(CallbackAcceptor.isAccepted(update, "invalid"));
    }

    @Test
    @DisplayName("Callback does not exist")
    void testIsAccepted_2() {
        when(update.callback()).thenReturn(null);

        assertFalse(CallbackAcceptor.isAccepted(update, "test"));
    }
}


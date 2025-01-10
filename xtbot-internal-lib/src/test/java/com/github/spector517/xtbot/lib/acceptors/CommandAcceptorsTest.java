package com.github.spector517.xtbot.lib.acceptors;

import com.github.spector517.xtbot.api.dto.Message;
import com.github.spector517.xtbot.api.dto.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandAcceptorsTest {

    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        update = mock(Update.class);
        message = mock(Message.class);
        when(update.message()).thenReturn(message);
    }

    @Test
    @DisplayName("Valid command")
    void testIsAccepted_0() {
        when(message.text()).thenReturn("/test");

        assertTrue(CommandAcceptors.isAccepted(update, "test"));
    }

    @Test
    @DisplayName("Invalid command 1")
    void testIsAccepted_1() {
        when(message.text()).thenReturn("/test");

        assertFalse(CommandAcceptors.isAccepted(update, "stop"));
    }

    @Test
    @DisplayName("Invalid command 2")
    void testIsAccepted_2() {
        when(message.text()).thenReturn("/supertest");

        assertFalse(CommandAcceptors.isAccepted(update, "test"));
    }

    @Test
    @DisplayName("Null message")
    void testIsAccepted_3() {
        when(update.message()).thenReturn(null);

        assertFalse(CommandAcceptors.isAccepted(update, "test"));
    }
}

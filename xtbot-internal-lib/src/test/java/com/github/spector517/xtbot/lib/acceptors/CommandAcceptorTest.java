package com.github.spector517.xtbot.lib.acceptors;

import com.github.spector517.xtbot.api.dto.Command;
import com.github.spector517.xtbot.api.dto.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandAcceptorTest {

    private Update update;
    private Command command;

    @BeforeEach
    void setUp() {
        update = mock(Update.class);
        command = mock(Command.class);
        when(update.command()).thenReturn(command);
    }

    @Test
    @DisplayName("Valid command")
    void testIsAccepted_0() {
        when(command.name()).thenReturn("test");

        assertTrue(CommandAcceptor.isAccepted(update, "test"));
    }

    @Test
    @DisplayName("Invalid command 1")
    void testIsAccepted_1() {
        when(command.name()).thenReturn("test");

        assertFalse(CommandAcceptor.isAccepted(update, "stop"));
    }

    @Test
    @DisplayName("Invalid command 2")
    void testIsAccepted_2() {
        when(command.name()).thenReturn("supertest");

        assertFalse(CommandAcceptor.isAccepted(update, "test"));
    }

    @Test
    @DisplayName("Null message")
    void testIsAccepted_3() {
        when(update.command()).thenReturn(null);

        assertFalse(CommandAcceptor.isAccepted(update, "test"));
    }
}

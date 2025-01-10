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

class MessageAcceptorsTest {

    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        update = mock(Update.class);
        message = mock(Message.class);
        when(update.message()).thenReturn(message);
    }

    @Test
    @DisplayName("Message exists and valid")
    void testIsAccepted_0() {
        var validMessage = "message";
        when(message.text()).thenReturn(validMessage);

        assertTrue(MessageAcceptors.isAccepted(update, validMessage));
    }

    @Test
    @DisplayName("Message exists and invalid")
    void testIsAccepted_1() {
        var validMessage = "message";
        var sentMessage = "other message";
        when(message.text()).thenReturn(validMessage);

        assertFalse(MessageAcceptors.isAccepted(update, sentMessage));
    }

    @Test
    @DisplayName("Message is not exists")
    void testIsAccepted_2() {
        var validMessage = "message";
        when(update.message()).thenReturn(null);

        assertFalse(MessageAcceptors.isAccepted(update, validMessage));
    }
}

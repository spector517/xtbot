package com.github.spector517.xtbot.core.repository;

import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import com.github.spector517.xtbot.core.repository.entity.MessageEntity;
import com.github.spector517.xtbot.core.repository.entity.MessageType;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class H2ClientRepositoryTest {

    private Path dbDirectory;
    private Path nonExistingDBDirectory;
    private Path regularFile;
    private LocalDateTime fixedTime;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        dbDirectory = Files.createTempDirectory("h2");
        nonExistingDBDirectory = Path.of("non", "existing", "db", "directory");
        regularFile = Files.createTempFile("h2", "db");
        fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        FileUtils.deleteDirectory(dbDirectory.toFile());
        FileUtils.deleteDirectory(Path.of("non").toFile());
    }

    @Test
    @DisplayName("Constructor with valid directory")
    void constructor_0() {
        assertDoesNotThrow(() -> new H2ClientRepository(dbDirectory));
        assertTrue(Files.exists(dbDirectory));
        assertTrue(Files.isDirectory(dbDirectory));
    }

    @Test
    @DisplayName("Constructor with non-existing directory")
    void constructor_1() {
        assertFalse(Files.exists(nonExistingDBDirectory));

        assertDoesNotThrow(() -> new H2ClientRepository(nonExistingDBDirectory));

        assertTrue(Files.exists(nonExistingDBDirectory));
        assertTrue(Files.isDirectory(nonExistingDBDirectory));
    }

    @Test
    @DisplayName("Constructor with regular file")
    void constructor_2() {
        assertThrows(IllegalStateException.class, () -> new H2ClientRepository(regularFile));
    }

    @Test
    @SneakyThrows
    @DisplayName("Client found")
    void findByExternalId_0() {
        var repository = new H2ClientRepository(dbDirectory);
        var clientEntity = getClientEntity().externalId(11L);
        repository.save(clientEntity);

        var foundClient = repository.findByExternalId(11L);

        assertEquals(clientEntity.externalId(), foundClient.externalId());
        assertEquals(clientEntity.name(), foundClient.name());
        assertEquals(clientEntity.stages(), foundClient.stages());
        assertEquals(clientEntity.messages().size(), foundClient.messages().size());
    }

    @Test
    @DisplayName("Client not found")
    void testFindByExternalId_1() {
        var repository = new H2ClientRepository(dbDirectory);

        assertThrows(
                ClientNotFoundException.class,
                () -> repository.findByExternalId(11L)
        );
    }

    @Test
    @DisplayName("Client updated")
    @SneakyThrows
    void save_0() {
        var repository = new H2ClientRepository(dbDirectory);
        var beforeUpdateClientEntity = getClientEntity()
                .externalId(11L)
                .stages(List.of("test1"));
        repository.save(beforeUpdateClientEntity);
        var updatedClientEntity = getClientEntity()
                .externalId(11L)
                .stages(List.of("test2"));

        repository.save(updatedClientEntity);

        var actualClientEntity = repository.findByExternalId(11L);
        assertEquals(List.of("test2"), actualClientEntity.stages());
    }

    @Test
    @DisplayName("Client added")
    @SneakyThrows
    void save_1() {
        var repository = new H2ClientRepository(dbDirectory);
        var firstClient = getClientEntity().externalId(11L);

        repository.save(firstClient);

        var secondClient = getClientEntity().externalId(22L);
        repository.save(secondClient);

        assertEquals(firstClient.externalId(), repository.findByExternalId(11L).externalId());
        assertEquals(secondClient.externalId(), repository.findByExternalId(22L).externalId());
    }

    @Test
    @DisplayName("New messages are added on save, old messages are not changed")
    @SneakyThrows
    void save_2() {
        var repository = new H2ClientRepository(dbDirectory);
        var initialMessage = new MessageEntity()
                .telegramMessageId(100)
                .text("initial message")
                .sentAt(fixedTime)
                .type(MessageType.BOT);
        var clientEntity = getClientEntity()
                .externalId(11L)
                .messages(List.of(initialMessage));
        repository.save(clientEntity);

        // Second save: same message ID (duplicate) + one new message
        var duplicateMessage = new MessageEntity()
                .telegramMessageId(100)
                .text("should not be duplicated")
                .sentAt(fixedTime)
                .type(MessageType.BOT);
        var newMessage = new MessageEntity()
                .telegramMessageId(200)
                .text("new message")
                .sentAt(fixedTime)
                .type(MessageType.USER);
        var updatedEntity = getClientEntity()
                .externalId(11L)
                .messages(List.of(duplicateMessage, newMessage));
        repository.save(updatedEntity);

        var result = repository.findByExternalId(11L);
        assertEquals(2, result.messages().size());
        assertEquals(100, result.messages().get(0).telegramMessageId());
        assertEquals("initial message", result.messages().get(0).text());
        assertEquals(200, result.messages().get(1).telegramMessageId());
        assertEquals("new message", result.messages().get(1).text());
    }

    @Test
    @DisplayName("Messages with null telegramMessageId are always added")
    @SneakyThrows
    void save_3() {
        var repository = new H2ClientRepository(dbDirectory);
        var callbackMsg = new MessageEntity()
                .telegramMessageId(null)
                .text("callback data")
                .sentAt(fixedTime)
                .type(MessageType.USER);
        var clientEntity = getClientEntity()
                .externalId(11L)
                .messages(List.of(callbackMsg));
        repository.save(clientEntity);

        // Second save with another null-id message
        var callbackMsg2 = new MessageEntity()
                .telegramMessageId(null)
                .text("another callback")
                .sentAt(fixedTime)
                .type(MessageType.USER);
        var updatedEntity = getClientEntity()
                .externalId(11L)
                .messages(List.of(callbackMsg2));
        repository.save(updatedEntity);

        var result = repository.findByExternalId(11L);
        assertEquals(2, result.messages().size());
    }

    private ClientEntity getClientEntity() {
        return new ClientEntity()
                .externalId(11L)
                .name("testClient")
                .messages(List.of())
                .stages(List.of("stage1", "stage2"))
                .stageInitiated(true)
                .stageCompleted(false)
                .additionalVars("var1=value1;var2=value2")
                .stageVars("stageVar1=value1;stageVar2=value2");
    }
}


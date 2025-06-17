package com.github.spector517.xtbot.core.repository;

import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class H2ClientRepositoryTest {

    private Path dbDirectory;
    private Path nonExistingDBDirectory;
    private Path regularFile;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        dbDirectory = Files.createTempDirectory("h2");
        nonExistingDBDirectory = Path.of("non", "existing", "db", "directory");
        regularFile = Files.createTempFile("h2", "db");
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
        var clientEntity = new ClientEntity().externalId(11L);
        repository.save(clientEntity);

        var foundClient = repository.findByExternalId(11L);

        assertEquals(clientEntity, foundClient);
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
        var clientEntity = new ClientEntity()
                .currentStage("test1")
                .externalId(11L);
        repository.save(clientEntity);
        var updatedClientEntity = new ClientEntity()
                .currentStage("test2")
                .externalId(11L);

        repository.save(updatedClientEntity);

        var actualClientEntity = repository.findByExternalId(11L);
        assertEquals(updatedClientEntity, actualClientEntity);
    }

    @Test
    @DisplayName("Client added")
    @SneakyThrows
    void save_1() {
        var repository = new H2ClientRepository(dbDirectory);
        var clientEntity = new ClientEntity()
                .externalId(11L);

        repository.save(clientEntity);

        var updatedClientEntity = new ClientEntity()
                .externalId(22L);

        repository.save(updatedClientEntity);

        assertEquals(clientEntity, repository.findByExternalId(11L));
        assertEquals(updatedClientEntity, repository.findByExternalId(22L));
    }
}